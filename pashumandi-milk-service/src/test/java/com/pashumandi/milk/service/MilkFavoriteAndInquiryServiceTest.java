package com.pashumandi.milk.service;

import com.pashumandi.milk.dto.favorite.MilkFavoriteResponse;
import com.pashumandi.milk.dto.inquiry.CreateInquiryRequest;
import com.pashumandi.milk.dto.inquiry.MilkInquiryResponse;
import com.pashumandi.milk.dto.listing.CreateMilkListingRequest;
import com.pashumandi.milk.dto.listing.MilkListingResponse;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.MilkType;
import com.pashumandi.milk.exception.BadRequestException;
import com.pashumandi.milk.exception.DuplicateResourceException;
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
class MilkFavoriteAndInquiryServiceTest {

    @Autowired
    private MilkFavoriteService milkFavoriteService;

    @Autowired
    private MilkInquiryService milkInquiryService;

    @Autowired
    private MilkListingService milkListingService;

    private AuthenticatedUser sellerUser;
    private AuthenticatedUser buyerUser;
    private MilkListingResponse testListing;

    @BeforeEach
    void setUp() {
        sellerUser = AuthenticatedUser.builder()
                .userId(801L)
                .role("SELLER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_SELLER")))
                .build();

        buyerUser = AuthenticatedUser.builder()
                .userId(901L)
                .role("BUYER")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUYER")))
                .build();

        CreateMilkListingRequest listingReq = CreateMilkListingRequest.builder()
                .title("A2 Gir Cow Milk - Fresh")
                .milkType(MilkType.COW)
                .pricePerLitre(new BigDecimal("75.00"))
                .availableQuantityLitres(new BigDecimal("25.00"))
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
    @DisplayName("Add to favorites and prevent duplicate favorite")
    void testFavoritesManagement() {
        MilkFavoriteResponse fav = milkFavoriteService.addFavorite(testListing.getId(), buyerUser);
        assertNotNull(fav.getId());
        assertEquals(buyerUser.getUserId(), fav.getUserId());
        assertEquals(testListing.getId(), fav.getListingId());

        // Duplicate favorite should be rejected
        assertThrows(DuplicateResourceException.class, () ->
                milkFavoriteService.addFavorite(testListing.getId(), buyerUser)
        );

        // Retrieve favorites
        var favorites = milkFavoriteService.getMyFavorites(PageRequest.of(0, 10), buyerUser);
        assertEquals(1, favorites.getContent().size());

        // Remove favorite
        milkFavoriteService.removeFavorite(testListing.getId(), buyerUser);
        var afterRemoval = milkFavoriteService.getMyFavorites(PageRequest.of(0, 10), buyerUser);
        assertTrue(afterRemoval.getContent().isEmpty());
    }

    @Test
    @DisplayName("Submit customer inquiry and retrieve in buyer/seller inboxes")
    void testInquiryManagement() {
        CreateInquiryRequest request = CreateInquiryRequest.builder()
                .message("Do you deliver by 6:30 AM?")
                .phone("9876543210")
                .build();

        MilkInquiryResponse inquiry = milkInquiryService.submitInquiry(testListing.getId(), request, buyerUser);
        assertNotNull(inquiry.getId());
        assertEquals("Do you deliver by 6:30 AM?", inquiry.getMessage());
        assertEquals(buyerUser.getUserId(), inquiry.getSenderUserId());
        assertEquals(sellerUser.getUserId(), inquiry.getSellerUserId());

        // Buyer inbox
        var buyerInquiries = milkInquiryService.getMyInquiries(PageRequest.of(0, 10), buyerUser);
        assertEquals(1, buyerInquiries.getContent().size());

        // Seller inbox
        var sellerInquiries = milkInquiryService.getReceivedInquiries(PageRequest.of(0, 10), sellerUser);
        assertEquals(1, sellerInquiries.getContent().size());

        // Seller cannot submit inquiry to self
        assertThrows(BadRequestException.class, () ->
                milkInquiryService.submitInquiry(testListing.getId(), request, sellerUser)
        );
    }
}
