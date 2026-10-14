package com.pashumandi.milk.controller;

import com.pashumandi.milk.dto.common.ApiResponse;
import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.listing.*;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.ListingStatus;
import com.pashumandi.milk.enums.MilkType;
import com.pashumandi.milk.security.AuthenticatedUser;
import com.pashumandi.milk.service.MilkListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/milk/listings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Milk Listings", description = "APIs for browsing, searching, creating, and managing milk listings")
public class MilkListingController {

    private final MilkListingService milkListingService;

    @PostMapping
    @Operation(summary = "Create a milk listing", description = "Creates a new milk listing owned by the authenticated seller.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Listing created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<MilkListingResponse>> createListing(
            @Valid @RequestBody CreateMilkListingRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkListingResponse response = milkListingService.createListing(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Milk listing created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Browse published milk listings", description = "Public access to browse published listings with stock availability.")
    public ResponseEntity<ApiResponse<PagedResponse<MilkListingResponse>>> browseListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) MilkType milkType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) FulfillmentType fulfillmentType
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        PagedResponse<MilkListingResponse> pagedResponse = milkListingService.browseListings(
                pageable, milkType, minPrice, maxPrice, state, district, pincode, fulfillmentType
        );

        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/search")
    @Operation(summary = "Search milk listings with advanced criteria", description = "Public endpoint for multi-attribute filtered search.")
    public ResponseEntity<ApiResponse<PagedResponse<MilkListingResponse>>> searchListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Search filters") MilkListingSearchCriteria criteria
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        PagedResponse<MilkListingResponse> pagedResponse = milkListingService.searchListings(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/my-listings")
    @Operation(summary = "Get current seller's listings", description = "Protected endpoint returning listings owned by the authenticated user.")
    public ResponseEntity<ApiResponse<PagedResponse<MilkListingResponse>>> getMyListings(
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        PagedResponse<MilkListingResponse> pagedResponse = milkListingService.getMyListings(status, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{listingId}")
    @Operation(summary = "Get listing details by ID", description = "Public details for published listings; authenticated owner/admin access for draft/paused listings.")
    public ResponseEntity<ApiResponse<MilkListingResponse>> getListingById(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkListingResponse response = milkListingService.getListingById(listingId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{listingId}")
    @Operation(summary = "Update a milk listing", description = "Requires seller ownership. Updates mutable listing details.")
    public ResponseEntity<ApiResponse<MilkListingResponse>> updateListing(
            @PathVariable Long listingId,
            @Valid @RequestBody UpdateMilkListingRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkListingResponse response = milkListingService.updateListing(listingId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Milk listing updated successfully", response));
    }

    @DeleteMapping("/{listingId}")
    @Operation(summary = "Deactivate a milk listing", description = "Soft-deactivates the listing to preserve order history.")
    public ResponseEntity<ApiResponse<Void>> deactivateListing(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        milkListingService.deactivateListing(listingId, currentUser);
        return ResponseEntity.ok(ApiResponse.messageOnly("Milk listing deactivated successfully"));
    }

    @PatchMapping("/{listingId}/status")
    @Operation(summary = "Publish or pause a listing", description = "Changes listing status (e.g. DRAFT -> PUBLISHED, PUBLISHED -> PAUSED).")
    public ResponseEntity<ApiResponse<MilkListingResponse>> updateListingStatus(
            @PathVariable Long listingId,
            @Valid @RequestBody UpdateListingStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkListingResponse response = milkListingService.updateListingStatus(listingId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Listing status updated successfully", response));
    }
}
