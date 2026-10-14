package com.pashumandi.milk.service.impl;

import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.listing.*;
import com.pashumandi.milk.entity.MilkListing;
import com.pashumandi.milk.enums.FulfillmentType;
import com.pashumandi.milk.enums.ListingStatus;
import com.pashumandi.milk.enums.MilkType;
import com.pashumandi.milk.exception.BadRequestException;
import com.pashumandi.milk.exception.ResourceNotFoundException;
import com.pashumandi.milk.exception.UnauthorizedAccessException;
import com.pashumandi.milk.repository.MilkListingRepository;
import com.pashumandi.milk.repository.specification.MilkListingSpecification;
import com.pashumandi.milk.security.AuthenticatedUser;
import com.pashumandi.milk.service.MilkListingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilkListingServiceImpl implements MilkListingService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "createdAt", "updatedAt", "pricePerLitre", "availableQuantityLitres", "title", "fatPercentage"
    );

    private final MilkListingRepository milkListingRepository;

    @Override
    @Transactional
    public MilkListingResponse createListing(CreateMilkListingRequest request, AuthenticatedUser currentUser) {
        log.info("Creating milk listing by user ID: {}", currentUser.getUserId());

        ListingStatus initialStatus = ListingStatus.DRAFT;
        if (Boolean.TRUE.equals(request.getPublishImmediately())) {
            if (request.getAvailableQuantityLitres().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Cannot publish listing immediately with zero available stock");
            }
            initialStatus = ListingStatus.PUBLISHED;
        }

        MilkListing listing = MilkListing.builder()
                .sellerUserId(currentUser.getUserId())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .milkType(request.getMilkType())
                .animalSource(request.getAnimalSource())
                .pricePerLitre(request.getPricePerLitre())
                .availableQuantityLitres(request.getAvailableQuantityLitres())
                .unit(request.getUnit() != null ? request.getUnit() : "LITRE")
                .fatPercentage(request.getFatPercentage())
                .snfPercentage(request.getSnfPercentage())
                .qualityDescription(request.getQualityDescription())
                .state(request.getState().trim())
                .district(request.getDistrict().trim())
                .village(request.getVillage().trim())
                .pincode(request.getPincode().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .fulfillmentType(request.getFulfillmentType())
                .availabilityStart(request.getAvailabilityStart())
                .availabilityEnd(request.getAvailabilityEnd())
                .status(initialStatus)
                .imageUrl(request.getImageUrl())
                .build();

        MilkListing saved = milkListingRepository.save(listing);
        log.info("Milk listing created with ID: {} and status: {}", saved.getId(), saved.getStatus());
        return MilkListingResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MilkListingResponse> browseListings(
            Pageable pageable,
            MilkType milkType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String state,
            String district,
            String pincode,
            FulfillmentType fulfillmentType
    ) {
        Pageable sanitizedPageable = sanitizePageable(pageable);

        MilkListingSearchCriteria criteria = MilkListingSearchCriteria.builder()
                .milkType(milkType)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .state(state)
                .district(district)
                .pincode(pincode)
                .fulfillmentType(fulfillmentType)
                .build();

        Specification<MilkListing> spec = MilkListingSpecification.buildSearchSpecification(criteria);
        Page<MilkListing> page = milkListingRepository.findAll(spec, sanitizedPageable);

        return PagedResponse.from(page, MilkListingResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MilkListingResponse> searchListings(MilkListingSearchCriteria criteria, Pageable pageable) {
        Pageable sanitizedPageable = sanitizePageable(pageable);
        Specification<MilkListing> spec = MilkListingSpecification.buildSearchSpecification(criteria);
        Page<MilkListing> page = milkListingRepository.findAll(spec, sanitizedPageable);
        return PagedResponse.from(page, MilkListingResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public MilkListingResponse getListingById(Long listingId, AuthenticatedUser currentUser) {
        MilkListing listing = milkListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk listing not found with ID: " + listingId));

        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            // Only owner or admin can view unpublished listings
            if (currentUser == null) {
                throw new UnauthorizedAccessException("Listing is not publicly available");
            }
            if (!listing.getSellerUserId().equals(currentUser.getUserId()) && !currentUser.isAdmin()) {
                throw new UnauthorizedAccessException("You do not have permission to view this unpublished listing");
            }
        }

        return MilkListingResponse.fromEntity(listing);
    }

    @Override
    @Transactional
    public MilkListingResponse updateListing(Long listingId, UpdateMilkListingRequest request, AuthenticatedUser currentUser) {
        MilkListing listing = milkListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk listing not found with ID: " + listingId));

        validateOwnership(listing, currentUser, "update");

        listing.setTitle(request.getTitle().trim());
        listing.setDescription(request.getDescription());
        listing.setMilkType(request.getMilkType());
        listing.setAnimalSource(request.getAnimalSource());
        listing.setPricePerLitre(request.getPricePerLitre());
        listing.setAvailableQuantityLitres(request.getAvailableQuantityLitres());
        listing.setUnit(request.getUnit() != null ? request.getUnit() : "LITRE");
        listing.setFatPercentage(request.getFatPercentage());
        listing.setSnfPercentage(request.getSnfPercentage());
        listing.setQualityDescription(request.getQualityDescription());
        listing.setState(request.getState().trim());
        listing.setDistrict(request.getDistrict().trim());
        listing.setVillage(request.getVillage().trim());
        listing.setPincode(request.getPincode().trim());
        listing.setLatitude(request.getLatitude());
        listing.setLongitude(request.getLongitude());
        listing.setFulfillmentType(request.getFulfillmentType());
        listing.setAvailabilityStart(request.getAvailabilityStart());
        listing.setAvailabilityEnd(request.getAvailabilityEnd());
        listing.setImageUrl(request.getImageUrl());

        // Automatic status reconciliation if stock changed
        if (listing.getStatus() == ListingStatus.SOLD_OUT && listing.getAvailableQuantityLitres().compareTo(BigDecimal.ZERO) > 0) {
            listing.setStatus(ListingStatus.PUBLISHED);
        } else if (listing.getStatus() == ListingStatus.PUBLISHED && listing.getAvailableQuantityLitres().compareTo(BigDecimal.ZERO) <= 0) {
            listing.setStatus(ListingStatus.SOLD_OUT);
        }

        MilkListing updated = milkListingRepository.save(listing);
        log.info("Updated milk listing ID: {}", updated.getId());
        return MilkListingResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deactivateListing(Long listingId, AuthenticatedUser currentUser) {
        MilkListing listing = milkListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk listing not found with ID: " + listingId));

        validateOwnership(listing, currentUser, "deactivate");

        listing.setStatus(ListingStatus.INACTIVE);
        milkListingRepository.save(listing);
        log.info("Soft-deactivated milk listing ID: {}", listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MilkListingResponse> getMyListings(ListingStatus status, Pageable pageable, AuthenticatedUser currentUser) {
        Pageable sanitizedPageable = sanitizePageable(pageable);
        Page<MilkListing> page;

        if (status != null) {
            page = milkListingRepository.findBySellerUserIdAndStatus(currentUser.getUserId(), status, sanitizedPageable);
        } else {
            page = milkListingRepository.findBySellerUserId(currentUser.getUserId(), sanitizedPageable);
        }

        return PagedResponse.from(page, MilkListingResponse::fromEntity);
    }

    @Override
    @Transactional
    public MilkListingResponse updateListingStatus(Long listingId, UpdateListingStatusRequest request, AuthenticatedUser currentUser) {
        MilkListing listing = milkListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Milk listing not found with ID: " + listingId));

        validateOwnership(listing, currentUser, "change status of");

        ListingStatus targetStatus = request.getStatus();
        ListingStatus currentStatus = listing.getStatus();

        if (currentStatus == targetStatus) {
            return MilkListingResponse.fromEntity(listing);
        }

        // Validate state transitions
        if (targetStatus == ListingStatus.PUBLISHED) {
            if (listing.getAvailableQuantityLitres().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Cannot publish listing with zero available stock");
            }
            if (listing.getPricePerLitre() == null || listing.getPricePerLitre().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Cannot publish listing without a valid positive price");
            }
        }

        listing.setStatus(targetStatus);
        MilkListing saved = milkListingRepository.save(listing);
        log.info("Updated listing {} status from {} to {}", listingId, currentStatus, targetStatus);
        return MilkListingResponse.fromEntity(saved);
    }

    private void validateOwnership(MilkListing listing, AuthenticatedUser currentUser, String action) {
        if (!listing.getSellerUserId().equals(currentUser.getUserId()) && !currentUser.isAdmin()) {
            throw new UnauthorizedAccessException("You do not have permission to " + action + " this listing");
        }
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int pageSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort();

        Sort safeSort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort.isSorted()) {
            safeSort = Sort.unsorted();
            for (Sort.Order order : sort) {
                if (ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                    safeSort = safeSort.and(Sort.by(order.getDirection(), order.getProperty()));
                }
            }
            if (!safeSort.isSorted()) {
                safeSort = Sort.by(Sort.Direction.DESC, "createdAt");
            }
        }

        return PageRequest.of(pageable.getPageNumber(), pageSize, safeSort);
    }
}
