package com.pashumandi.milk.service;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.listing.*;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.ListingStatus;
import com.pashumandi.milk.enums.MilkType;
import com.pashumandi.milk.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface MilkListingService {

    MilkListingResponse createListing(CreateMilkListingRequest request, AuthenticatedUser currentUser);

    PagedResponse<MilkListingResponse> browseListings(
            Pageable pageable,
            MilkType milkType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String state,
            String district,
            String pincode,
            FulfillmentType fulfillmentType
    );

    PagedResponse<MilkListingResponse> searchListings(MilkListingSearchCriteria criteria, Pageable pageable);

    MilkListingResponse getListingById(Long listingId, AuthenticatedUser currentUser);

    MilkListingResponse updateListing(Long listingId, UpdateMilkListingRequest request, AuthenticatedUser currentUser);

    void deactivateListing(Long listingId, AuthenticatedUser currentUser);

    PagedResponse<MilkListingResponse> getMyListings(ListingStatus status, Pageable pageable, AuthenticatedUser currentUser);

    MilkListingResponse updateListingStatus(Long listingId, UpdateListingStatusRequest request, AuthenticatedUser currentUser);
}
