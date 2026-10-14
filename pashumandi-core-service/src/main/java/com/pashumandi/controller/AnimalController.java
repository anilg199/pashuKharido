package com.pashumandi.controller;

import com.pashumandi.dto.animal.*;
import com.pashumandi.dto.common.ApiResponse;
import com.pashumandi.dto.common.PagedResponse;
import com.pashumandi.enums.AnimalGender;
import com.pashumandi.enums.AnimalType;
import com.pashumandi.enums.UserRole;
import com.pashumandi.security.UserPrincipal;
import com.pashumandi.service.AnimalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/animals")
@RequiredArgsConstructor
@Tag(name = "Animal Listing Management", description = "Endpoints for creating, managing, browsing, and searching livestock listings")
public class AnimalController {

    private final AnimalService animalService;

    @PostMapping
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Create an animal listing",
            description = "Creates a new livestock listing assigned to the authenticated user. Never trusts client-supplied seller IDs. Initial status defaults to DRAFT unless PUBLISHED is explicitly requested and validated."
    )
    public ResponseEntity<ApiResponse<AnimalResponse>> createAnimal(
            @Valid @RequestBody AnimalCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AnimalResponse response = animalService.createAnimal(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Animal listing created successfully", response));
    }

    @GetMapping
    @Operation(
            summary = "Browse published animal listings",
            description = "Publicly browses published livestock listings with pagination and sorting. Excludes draft, inactive, and sold listings."
    )
    public ResponseEntity<ApiResponse<PagedResponse<AnimalResponse>>> getPublishedAnimals(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)", example = "12")
            @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "Sort property (createdAt, price, title, ageMonths, updatedAt)", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        PagedResponse<AnimalResponse> response = animalService.getPublishedAnimals(page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/featured")
    @Operation(
            summary = "Get featured animal listings",
            description = "Returns a deterministic list of featured published listings (most recently published animals with primary images)."
    )
    public ResponseEntity<ApiResponse<List<AnimalSummaryResponse>>> getFeaturedListings(
            @Parameter(description = "Number of featured listings to return (max 20)", example = "6")
            @RequestParam(defaultValue = "6") int limit
    ) {
        List<AnimalSummaryResponse> responses = animalService.getFeaturedListings(limit);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search and filter animal listings",
            description = "Searches published livestock listings by category, breed, gender, price range, location, and livestock attributes."
    )
    public ResponseEntity<ApiResponse<PagedResponse<AnimalResponse>>> searchAnimals(
            @Parameter(description = "Category of animal (COW, BUFFALO, GOAT)")
            @RequestParam(required = false) AnimalType animalType,
            @Parameter(description = "Breed (case-insensitive substring match)")
            @RequestParam(required = false) String breed,
            @Parameter(description = "Gender of animal (MALE, FEMALE)")
            @RequestParam(required = false) AnimalGender gender,
            @Parameter(description = "Minimum price in INR")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price in INR")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Minimum age in months")
            @RequestParam(required = false) Integer minAgeMonths,
            @Parameter(description = "Maximum age in months")
            @RequestParam(required = false) Integer maxAgeMonths,
            @Parameter(description = "State name (partial match)")
            @RequestParam(required = false) String state,
            @Parameter(description = "District name (partial match)")
            @RequestParam(required = false) String district,
            @Parameter(description = "Postal PIN code")
            @RequestParam(required = false) String pincode,
            @Parameter(description = "Minimum milk yield in litres per day")
            @RequestParam(required = false) BigDecimal minMilkYield,
            @Parameter(description = "Maximum milk yield in litres per day")
            @RequestParam(required = false) BigDecimal maxMilkYield,
            @Parameter(description = "Pregnancy status filter")
            @RequestParam(required = false) Boolean pregnant,
            @Parameter(description = "Vaccination status filter")
            @RequestParam(required = false) Boolean vaccinated,
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)")
            @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "Sort property")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)")
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        PagedResponse<AnimalResponse> response = animalService.searchAnimals(
                animalType, breed, gender, minPrice, maxPrice, minAgeMonths, maxAgeMonths,
                state, district, pincode, minMilkYield, maxMilkYield, pregnant, vaccinated,
                page, size, sortBy, direction
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/nearby")
    @Operation(
            summary = "Find nearby animal listings",
            description = "Finds published listings located within the specified radius (in km) from given latitude and longitude coordinates, sorted by distance."
    )
    public ResponseEntity<ApiResponse<PagedResponse<AnimalResponse>>> getNearbyAnimals(
            @Parameter(description = "Latitude (-90 to +90)", required = true, example = "26.8500000")
            @RequestParam Double latitude,
            @Parameter(description = "Longitude (-180 to +180)", required = true, example = "83.9800000")
            @RequestParam Double longitude,
            @Parameter(description = "Search radius in kilometers (max 500)", example = "50.0")
            @RequestParam(defaultValue = "50.0") Double radiusKm,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)", example = "12")
            @RequestParam(defaultValue = "12") int size
    ) {
        PagedResponse<AnimalResponse> response = animalService.getNearbyAnimals(latitude, longitude, radiusKm, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-listings")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Get authenticated seller's listings",
            description = "Retrieves all listings owned by the authenticated seller across all statuses (DRAFT, PUBLISHED, SOLD, INACTIVE)."
    )
    public ResponseEntity<ApiResponse<PagedResponse<AnimalResponse>>> getMyListings(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)", example = "12")
            @RequestParam(defaultValue = "12") int size,
            @Parameter(description = "Sort property", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String direction,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PagedResponse<AnimalResponse> response = animalService.getMyListings(principal.getId(), page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{animalId}")
    @Operation(
            summary = "Get animal listing details",
            description = "Retrieves detailed information for an animal listing including ordered images. Public for PUBLISHED listings; unpublished listings require owner or ADMIN authentication."
    )
    public ResponseEntity<ApiResponse<AnimalResponse>> getAnimalDetails(
            @Parameter(description = "ID of the animal listing", required = true)
            @PathVariable Long animalId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        boolean isAdmin = principal != null && principal.getRole() == UserRole.ADMIN;
        AnimalResponse response = animalService.getAnimalDetails(animalId, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{animalId}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Update an animal listing",
            description = "Updates listing details. Only the listing owner or ADMIN can perform updates. Changing the seller ID is prevented."
    )
    public ResponseEntity<ApiResponse<AnimalResponse>> updateAnimal(
            @Parameter(description = "ID of the animal listing", required = true)
            @PathVariable Long animalId,
            @Valid @RequestBody AnimalUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal.getRole() == UserRole.ADMIN;
        AnimalResponse response = animalService.updateAnimal(animalId, request, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Animal listing updated successfully", response));
    }

    @DeleteMapping("/{animalId}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Deactivate an animal listing",
            description = "Soft-deletes an animal listing by transitioning its status to INACTIVE. Preserves associated image records and Cloudinary assets."
    )
    public ResponseEntity<ApiResponse<Void>> deactivateAnimal(
            @Parameter(description = "ID of the animal listing", required = true)
            @PathVariable Long animalId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal.getRole() == UserRole.ADMIN;
        animalService.deactivateAnimal(animalId, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.messageOnly("Animal listing deactivated successfully"));
    }

    @PatchMapping("/{animalId}/status")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Update listing status",
            description = "Transitions listing lifecycle status (e.g., mark as SOLD, PUBLISHED, INACTIVE, DRAFT). Enforces strict transition validation rules."
    )
    public ResponseEntity<ApiResponse<AnimalResponse>> updateAnimalStatus(
            @Parameter(description = "ID of the animal listing", required = true)
            @PathVariable Long animalId,
            @Valid @RequestBody AnimalStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal.getRole() == UserRole.ADMIN;
        AnimalResponse response = animalService.updateAnimalStatus(animalId, request.getStatus(), principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Animal listing status updated successfully", response));
    }
}
