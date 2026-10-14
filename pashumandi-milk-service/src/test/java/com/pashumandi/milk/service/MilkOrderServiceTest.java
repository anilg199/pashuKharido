package com.pashumandi.milk.service;

import com.pashumandi.milk.dto.listing.CreateMilkListingRequest;
import com.pashumandi.milk.dto.listing.MilkListingResponse;
import com.pashumandi.milk.dto.order.*;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.ListingStatus;
import com.pashumandi.milk.enums.MilkType;
import com.pashumandi.milk.enums.OrderStatus;
import com.pashumandi.milk.exception.BadRequestException;
import com.pashumandi.milk.exception.InsufficientStockException;
import com.pashumandi.milk.exception.InvalidOrderStateTransitionException;
import com.pashumandi.milk.exception.UnauthorizedAccessException;
import com.pashumandi.milk.repository.MilkListingRepository;
import com.pashumandi.milk.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MilkOrderServiceTest {

    @Autowired
    private MilkOrderService milkOrderService;

    @Autowired
    private MilkListingService milkListingService;

    @Autowired
    private MilkListingRepository milkListingRepository;

    private AuthenticatedUser sellerUser;
    private AuthenticatedUser buyerUser;
    private AuthenticatedUser strangerUser;

    private MilkListingResponse testListing;

    @BeforeEach
    void setUp() {
        sellerUser = AuthenticatedUser.builder()
                .userId(101L)
                .role("SELLER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_SELLER")))
                .build();

        buyerUser = AuthenticatedUser.builder()
                .userId(202L)
                .role("BUYER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER")))
                .build();

        strangerUser = AuthenticatedUser.builder()
                .userId(303L)
                .role("BUYER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER")))
                .build();

        CreateMilkListingRequest listingReq = CreateMilkListingRequest.builder()
                .title("Fresh Desi Cow Milk")
                .milkType(MilkType.COW)
                .pricePerLitre(new BigDecimal("60.00"))
                .availableQuantityLitres(new BigDecimal("10.00"))
                .state("UP")
                .district("Kushinagar")
                .village("Ramkola")
                .pincode("274401")
                .fulfillmentType(FulfillmentType.BOTH)
                .publishImmediately(true)
                .build();

        testListing = milkListingService.createListing(listingReq, sellerUser);
    }

    @Test
    @DisplayName("1. Place a valid order with inventory deduction and server total calculation")
    void testPlaceValidOrder() {
        CreateMilkOrderRequest orderReq = CreateMilkOrderRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("3.00"))
                .fulfillmentType(FulfillmentType.DELIVERY)
                .deliveryAddress(DeliveryAddressDto.builder()
                        .state("UP")
                        .district("Kushinagar")
                        .village("Ramkola")
                        .pincode("274401")
                        .build())
                .build();

        MilkOrderResponse order = milkOrderService.placeOrder(orderReq, buyerUser);

        assertNotNull(order.getId());
        assertNotNull(order.getOrderNumber());
        assertEquals(new BigDecimal("3.00"), order.getQuantityLitres());
        assertEquals(new BigDecimal("60.00"), order.getUnitPrice());
        // 60.00 * 3.00 = 180.00
        assertEquals(new BigDecimal("180.00"), order.getTotalPrice());
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());

        // Verify remaining stock is 7.00 L
        var updatedListing = milkListingRepository.findById(testListing.getId()).orElseThrow();
        assertEquals(new BigDecimal("7.00"), updatedListing.getAvailableQuantityLitres());
    }

    @Test
    @DisplayName("2. Reject order for unpublished or draft listings")
    void testRejectOrderForDraftListing() {
        CreateMilkListingRequest draftReq = CreateMilkListingRequest.builder()
                .title("Draft Goat Milk")
                .milkType(MilkType.GOAT)
                .pricePerLitre(new BigDecimal("100.00"))
                .availableQuantityLitres(new BigDecimal("5.00"))
                .state("UP")
                .district("Kushinagar")
                .village("Ramkola")
                .pincode("274401")
                .fulfillmentType(FulfillmentType.PICKUP)
                .publishImmediately(false) // DRAFT
                .build();

        MilkListingResponse draftListing = milkListingService.createListing(draftReq, sellerUser);

        CreateMilkOrderRequest orderReq = CreateMilkOrderRequest.builder()
                .listingId(draftListing.getId())
                .quantityLitres(new BigDecimal("1.00"))
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        assertThrows(BadRequestException.class, () -> milkOrderService.placeOrder(orderReq, buyerUser));
    }

    @Test
    @DisplayName("3. Reject order exceeding available stock")
    void testRejectOrderExceedingStock() {
        CreateMilkOrderRequest orderReq = CreateMilkOrderRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("15.00")) // Listing only has 10.00 L
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        assertThrows(InsufficientStockException.class, () -> milkOrderService.placeOrder(orderReq, buyerUser));
    }

    @Test
    @DisplayName("4. Reject self-purchase: seller cannot order from own listing")
    void testRejectSelfPurchase() {
        CreateMilkOrderRequest orderReq = CreateMilkOrderRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("1.00"))
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        assertThrows(BadRequestException.class, () -> milkOrderService.placeOrder(orderReq, sellerUser));
    }

    @Test
    @DisplayName("5. Reject unauthorized access to another user's order")
    void testRejectUnauthorizedOrderAccess() {
        CreateMilkOrderRequest orderReq = CreateMilkOrderRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("2.00"))
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        MilkOrderResponse order = milkOrderService.placeOrder(orderReq, buyerUser);

        // strangerUser (ID 303) is neither buyer nor seller
        assertThrows(UnauthorizedAccessException.class, () ->
                milkOrderService.getOrderById(order.getId(), strangerUser)
        );

        // Buyer can access
        assertNotNull(milkOrderService.getOrderById(order.getId(), buyerUser));

        // Seller can access
        assertNotNull(milkOrderService.getOrderById(order.getId(), sellerUser));
    }

    @Test
    @DisplayName("6. Validate order status transitions and safe stock restoration on cancellation")
    void testOrderStatusLifecycleAndCancellationRestoration() {
        CreateMilkOrderRequest orderReq = CreateMilkOrderRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("4.00"))
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        MilkOrderResponse order = milkOrderService.placeOrder(orderReq, buyerUser);
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());

        // Stock decreased from 10 to 6
        var listingAfterOrder = milkListingRepository.findById(testListing.getId()).orElseThrow();
        assertEquals(new BigDecimal("6.00"), listingAfterOrder.getAvailableQuantityLitres());

        // Seller accepts order
        UpdateOrderStatusRequest acceptReq = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.ACCEPTED)
                .build();
        MilkOrderResponse accepted = milkOrderService.updateOrderStatus(order.getId(), acceptReq, sellerUser);
        assertEquals(OrderStatus.ACCEPTED, accepted.getOrderStatus());

        // Buyer cancels accepted order -> stock must be restored to 10
        CancelOrderRequest cancelReq = CancelOrderRequest.builder()
                .reason("Need to cancel today")
                .build();
        MilkOrderResponse cancelled = milkOrderService.cancelOrder(order.getId(), cancelReq, buyerUser);
        assertEquals(OrderStatus.CANCELLED, cancelled.getOrderStatus());

        var listingAfterCancel = milkListingRepository.findById(testListing.getId()).orElseThrow();
        assertEquals(new BigDecimal("10.00"), listingAfterCancel.getAvailableQuantityLitres());
    }

    @Test
    @DisplayName("7. Delivered order cannot be cancelled through ordinary update")
    void testDeliveredOrderCannotBeCancelled() {
        CreateMilkOrderRequest orderReq = CreateMilkOrderRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("1.00"))
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        MilkOrderResponse order = milkOrderService.placeOrder(orderReq, buyerUser);

        // PENDING -> ACCEPTED
        milkOrderService.updateOrderStatus(order.getId(), UpdateOrderStatusRequest.builder().status(OrderStatus.ACCEPTED).build(), sellerUser);
        // ACCEPTED -> READY_FOR_PICKUP
        milkOrderService.updateOrderStatus(order.getId(), UpdateOrderStatusRequest.builder().status(OrderStatus.READY_FOR_PICKUP).build(), sellerUser);
        // READY_FOR_PICKUP -> DELIVERED
        milkOrderService.updateOrderStatus(order.getId(), UpdateOrderStatusRequest.builder().status(OrderStatus.DELIVERED).build(), sellerUser);

        // Attempting to cancel delivered order -> InvalidOrderStateTransitionException
        assertThrows(InvalidOrderStateTransitionException.class, () ->
                milkOrderService.updateOrderStatus(order.getId(), UpdateOrderStatusRequest.builder().status(OrderStatus.CANCELLED).build(), buyerUser)
        );
    }
}
