package com.pashumandi.milk.service;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.listing.*;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.ListingStatus;
import com.pashumandi.milk.enums.MilkType;
import com.pashumandi.milk.exception.BadRequestException;
import com.pashumandi.milk.exception.UnauthorizedAccessException;
import com.pashumandi.milk.repository.MilkListingRepository;
import com.pashumandi.milk.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MilkListingServiceTest {

    @Autowired
    private MilkListingService milkListingService;

    @Autowired
    private MilkListingRepository milkListingRepository;

    private AuthenticatedUser sellerUser;
    private AuthenticatedUser otherUser;
    private AuthenticatedUser adminUser;

    @BeforeEach
    void setUp() {
        sellerUser = AuthenticatedUser.builder()
                .userId(100L)
                .role("SELLER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_SELLER")))
                .build();

        otherUser = AuthenticatedUser.builder()
                .userId(200L)
                .role("BUYER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER")))
                .build();

        adminUser = AuthenticatedUser.builder()
                .userId(999L)
                .role("ADMIN")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
    }

    @Test
    @DisplayName("1. Create a valid milk listing in DRAFT status by default")
    void testCreateValidListing() {
        CreateMilkListingRequest request = CreateMilkListingRequest.builder()
                .title("Organic Raw Cow Milk")
                .description("Farm fresh daily")
                .milkType(MilkType.COW)
                .animalSource("Sahiwal")
                .pricePerLitre(new BigDecimal("65.00"))
                .availableQuantityLitres(new BigDecimal("30.00"))
                .fatPercentage(new BigDecimal("4.2"))
                .snfPercentage(new BigDecimal("8.5"))
                .state("Uttar Pradesh")
                .district("Kushinagar")
                .village("Ramkola")
                .pincode("274401")
                .fulfillmentType(FulfillmentType.BOTH)
                .build();

        MilkListingResponse response = milkListingService.createListing(request, sellerUser);

        assertNotNull(response.getId());
        assertEquals(sellerUser.getUserId(), response.getSellerUserId());
        assertEquals("Organic Raw Cow Milk", response.getTitle());
        assertEquals(ListingStatus.DRAFT, response.getStatus());
        assertEquals(new BigDecimal("30.00"), response.getAvailableQuantityLitres());
    }

    @Test
    @DisplayName("2. Reject publishing listing with zero available stock")
    void testRejectPublishImmediatelyWithZeroStock() {
        CreateMilkListingRequest request = CreateMilkListingRequest.builder()
                .title("Zero Stock Milk")
                .milkType(MilkType.COW)
                .pricePerLitre(new BigDecimal("60.00"))
                .availableQuantityLitres(BigDecimal.ZERO)
                .state("UP")
                .district("Kushinagar")
                .village("Ramkola")
                .pincode("274401")
                .fulfillmentType(FulfillmentType.PICKUP)
                .publishImmediately(true)
                .build();

        assertThrows(BadRequestException.class, () -> milkListingService.createListing(request, sellerUser));
    }

    @Test
    @DisplayName("3. Publish and deactivate listings correctly")
    void testPublishAndDeactivateListing() {
        CreateMilkListingRequest createReq = CreateMilkListingRequest.builder()
                .title("Murrah Buffalo Milk")
                .milkType(MilkType.BUFFALO)
                .pricePerLitre(new BigDecimal("80.00"))
                .availableQuantityLitres(new BigDecimal("20.00"))
                .state("UP")
                .district("Kushinagar")
                .village("Ramkola")
                .pincode("274401")
                .fulfillmentType(FulfillmentType.DELIVERY)
                .build();

        MilkListingResponse created = milkListingService.createListing(createReq, sellerUser);
        assertEquals(ListingStatus.DRAFT, created.getStatus());

        // Publish
        UpdateListingStatusRequest publishReq = UpdateListingStatusRequest.builder()
                .status(ListingStatus.PUBLISHED)
                .build();
        MilkListingResponse published = milkListingService.updateListingStatus(created.getId(), publishReq, sellerUser);
        assertEquals(ListingStatus.PUBLISHED, published.getStatus());

        // Deactivate (soft delete)
        milkListingService.deactivateListing(created.getId(), sellerUser);
        MilkListingResponse deactivated = milkListingService.getListingById(created.getId(), sellerUser);
        assertEquals(ListingStatus.INACTIVE, deactivated.getStatus());
    }

    @Test
    @DisplayName("4. Browse returns published listings only; drafts and inactive listings are excluded")
    void testBrowsePublishedListingsOnly() {
        // Create 1 draft listing
        CreateMilkListingRequest draftReq = CreateMilkListingRequest.builder()
                .title("Draft Cow Milk")
                .milkType(MilkType.COW)
                .pricePerLitre(new BigDecimal("60.00"))
                .availableQuantityLitres(new BigDecimal("15.00"))
                .state("UP")
                .district("Kushinagar")
                .village("Ramkola")
                .pincode("274401")
                .fulfillmentType(FulfillmentType.BOTH)
                .build();
        milkListingService.createListing(draftReq, sellerUser);

        // Create 1 published listing
        CreateMilkListingRequest pubReq = CreateMilkListingRequest.builder()
                .title("Published Goat Milk")
                .milkType(MilkType.GOAT)
                .pricePerLitre(new BigDecimal("120.00"))
                .availableQuantityLitres(new BigDecimal("10.00"))
                .state("UP")
                .district("Kushinagar")
                .village("Ramkola")
                .pincode("274401")
                .fulfillmentType(FulfillmentType.BOTH)
                .publishImmediately(true)
                .build();
        milkListingService.createListing(pubReq, sellerUser);

        PagedResponse<MilkListingResponse> browseResults = milkListingService.browseListings(
                PageRequest.of(0, 10), null, null, null, null, null, null, null
        );

        assertNotNull(browseResults);
        assertTrue(browseResults.getContent().stream().allMatch(l -> l.getStatus() == ListingStatus.PUBLISHED));
        assertTrue(browseResults.getContent().stream().noneMatch(l -> l.getTitle().equals("Draft Cow Milk")));
    }

    @Test
    @DisplayName("5. Search with filters and pagination works accurately")
    void testSearchWithFilters() {
        CreateMilkListingRequest req1 = CreateMilkListingRequest.builder()
                .title("Affordable Cow Milk")
                .milkType(MilkType.COW)
                .pricePerLitre(new BigDecimal("50.00"))
                .availableQuantityLitres(new BigDecimal("20.00"))
                .state("Bihar")
                .district("Patna")
                .village("Danapur")
                .pincode("801503")
                .fulfillmentType(FulfillmentType.DELIVERY)
                .publishImmediately(true)
                .build();
        milkListingService.createListing(req1, sellerUser);

        MilkListingSearchCriteria criteria = MilkListingSearchCriteria.builder()
                .milkType(MilkType.COW)
                .maxPrice(new BigDecimal("55.00"))
                .state("Bihar")
                .build();

        PagedResponse<MilkListingResponse> searchResults = milkListingService.searchListings(criteria, PageRequest.of(0, 10));
        assertFalse(searchResults.getContent().isEmpty());
        assertTrue(searchResults.getContent().stream().allMatch(l -> l.getMilkType() == MilkType.COW));
    }

    @Test
    @DisplayName("6. Enforce seller ownership on updates and prevent unauthorized modification")
    void testEnforceSellerOwnership() {
        CreateMilkListingRequest req = CreateMilkListingRequest.builder()
                .title("Seller Listing")
                .milkType(MilkType.COW)
                .pricePerLitre(new BigDecimal("60.00"))
                .availableQuantityLitres(new BigDecimal("10.00"))
                .state("UP")
                .district("Kushinagar")
                .village("Ramkola")
                .pincode("274401")
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        MilkListingResponse listing = milkListingService.createListing(req, sellerUser);

        UpdateMilkListingRequest updateReq = UpdateMilkListingRequest.builder()
                .title("Hacked Title")
                .milkType(MilkType.COW)
                .pricePerLitre(new BigDecimal("10.00"))
                .availableQuantityLitres(new BigDecimal("100.00"))
                .state("UP")
                .district("Kushinagar")
                .village("Ramkola")
                .pincode("274401")
                .fulfillmentType(FulfillmentType.PICKUP)
                .build();

        // otherUser attempts to update sellerUser's listing -> UnauthorizedAccessException
        assertThrows(UnauthorizedAccessException.class, () ->
                milkListingService.updateListing(listing.getId(), updateReq, otherUser)
        );

        // sellerUser updates own listing -> Success
        updateReq.setTitle("Legitimate Update");
        MilkListingResponse updated = milkListingService.updateListing(listing.getId(), updateReq, sellerUser);
        assertEquals("Legitimate Update", updated.getTitle());
    }
}
