package com.pashumandi.milk.service;

import com.pashumandi.milk.dto.listing.CreateMilkListingRequest;
import com.pashumandi.milk.dto.listing.MilkListingResponse;
import com.pashumandi.milk.dto.order.DeliveryAddressDto;
import com.pashumandi.milk.dto.subscription.CreateSubscriptionRequest;
import com.pashumandi.milk.dto.subscription.MilkSubscriptionResponse;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.MilkType;
import com.pashumandi.milk.enums.SubscriptionFrequency;
import com.pashumandi.milk.enums.SubscriptionStatus;
import com.pashumandi.milk.exception.BadRequestException;
import com.pashumandi.milk.exception.InvalidSubscriptionStateException;
import com.pashumandi.milk.exception.UnauthorizedAccessException;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MilkSubscriptionServiceTest {

    @Autowired
    private MilkSubscriptionService milkSubscriptionService;

    @Autowired
    private MilkListingService milkListingService;

    private AuthenticatedUser sellerUser;
    private AuthenticatedUser buyerUser;
    private AuthenticatedUser otherUser;
    private MilkListingResponse testListing;

    @BeforeEach
    void setUp() {
        sellerUser = AuthenticatedUser.builder()
                .userId(501L)
                .role("SELLER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_SELLER")))
                .build();

        buyerUser = AuthenticatedUser.builder()
                .userId(601L)
                .role("BUYER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER")))
                .build();

        otherUser = AuthenticatedUser.builder()
                .userId(701L)
                .role("BUYER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER")))
                .build();

        CreateMilkListingRequest listingReq = CreateMilkListingRequest.builder()
                .title("Subscription Eligible Cow Milk")
                .milkType(MilkType.COW)
                .pricePerLitre(new BigDecimal("65.00"))
                .availableQuantityLitres(new BigDecimal("50.00"))
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
    @DisplayName("1. Create a recurring daily milk subscription")
    void testCreateSubscription() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("1.50"))
                .frequency(SubscriptionFrequency.DAILY)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .preferredTimeSlot("MORNING_07_00")
                .fulfillmentType(FulfillmentType.DELIVERY)
                .deliveryAddress(DeliveryAddressDto.builder()
                        .state("UP")
                        .district("Kushinagar")
                        .village("Ramkola")
                        .pincode("274401")
                        .build())
                .build();

        MilkSubscriptionResponse response = milkSubscriptionService.createSubscription(request, buyerUser);

        assertNotNull(response.getId());
        assertNotNull(response.getSubscriptionNumber());
        assertEquals(buyerUser.getUserId(), response.getBuyerUserId());
        assertEquals(sellerUser.getUserId(), response.getSellerUserId());
        assertEquals(SubscriptionStatus.ACTIVE, response.getStatus());
        assertEquals(new BigDecimal("1.50"), response.getQuantityLitres());
    }

    @Test
    @DisplayName("2. Reject invalid scheduling: end date cannot precede start date")
    void testRejectEndDateBeforeStartDate() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("1.00"))
                .frequency(SubscriptionFrequency.DAILY)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(5)) // Invalid: end < start
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        assertThrows(BadRequestException.class, () ->
                milkSubscriptionService.createSubscription(request, buyerUser)
        );
    }

    @Test
    @DisplayName("3. Pause and resume a subscription")
    void testPauseAndResumeSubscription() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("2.00"))
                .frequency(SubscriptionFrequency.DAILY)
                .startDate(LocalDate.now().plusDays(1))
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        MilkSubscriptionResponse sub = milkSubscriptionService.createSubscription(request, buyerUser);
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());

        // Pause
        MilkSubscriptionResponse paused = milkSubscriptionService.pauseSubscription(sub.getId(), buyerUser);
        assertEquals(SubscriptionStatus.PAUSED, paused.getStatus());

        // Cannot pause already paused
        assertThrows(InvalidSubscriptionStateException.class, () ->
                milkSubscriptionService.pauseSubscription(sub.getId(), buyerUser)
        );

        // Resume
        MilkSubscriptionResponse resumed = milkSubscriptionService.resumeSubscription(sub.getId(), buyerUser);
        assertEquals(SubscriptionStatus.ACTIVE, resumed.getStatus());
    }

    @Test
    @DisplayName("4. Cancel subscription")
    void testCancelSubscription() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("1.00"))
                .frequency(SubscriptionFrequency.WEEKLY)
                .startDate(LocalDate.now().plusDays(1))
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        MilkSubscriptionResponse sub = milkSubscriptionService.createSubscription(request, buyerUser);

        MilkSubscriptionResponse cancelled = milkSubscriptionService.cancelSubscription(sub.getId(), buyerUser);
        assertEquals(SubscriptionStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @DisplayName("5. Enforce participant ownership on subscription details and operations")
    void testEnforceSubscriptionOwnership() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("1.00"))
                .frequency(SubscriptionFrequency.DAILY)
                .startDate(LocalDate.now().plusDays(1))
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        MilkSubscriptionResponse sub = milkSubscriptionService.createSubscription(request, buyerUser);

        // otherUser cannot view or pause
        assertThrows(UnauthorizedAccessException.class, () ->
                milkSubscriptionService.getSubscriptionById(sub.getId(), otherUser)
        );
        assertThrows(UnauthorizedAccessException.class, () ->
                milkSubscriptionService.pauseSubscription(sub.getId(), otherUser)
        );

        // Buyer and Seller can access
        assertNotNull(milkSubscriptionService.getSubscriptionById(sub.getId(), buyerUser));
        assertNotNull(milkSubscriptionService.getSubscriptionById(sub.getId(), sellerUser));
    }

    @Test
    @DisplayName("6. Verify scheduled order generation executes properly")
    void testScheduledOrderGeneration() {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .listingId(testListing.getId())
                .quantityLitres(new BigDecimal("2.00"))
                .frequency(SubscriptionFrequency.DAILY)
                .startDate(LocalDate.now()) // Active today
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        milkSubscriptionService.createSubscription(request, buyerUser);

        List<Long> generatedOrderIds = milkSubscriptionService.generateDailyOrdersForSubscriptions(LocalDate.now());
        assertFalse(generatedOrderIds.isEmpty());
    }
}
