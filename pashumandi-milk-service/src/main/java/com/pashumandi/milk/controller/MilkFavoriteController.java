package com.pashumandi.milk.controller;

import com.pashumandi.milk.dto.common.ApiResponse;
import com.pashumandi.milk.dto.common.PagedResponse;
import com.pashumandi.milk.dto.favorite.MilkFavoriteResponse;
import com.pashumandi.milk.security.AuthenticatedUser;
import com.pashumandi.milk.service.MilkFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/milk/favorites")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Milk Favorites", description = "Buyer wishlist and favorite listings management")
public class MilkFavoriteController {

    private final MilkFavoriteService milkFavoriteService;

    @PostMapping("/{listingId}")
    @Operation(summary = "Add listing to favorites", description = "Saves a milk listing to current user's favorites.")
    public ResponseEntity<ApiResponse<MilkFavoriteResponse>> addFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        MilkFavoriteResponse response = milkFavoriteService.addFavorite(listingId, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Listing added to favorites", response));
    }

    @DeleteMapping("/{listingId}")
    @Operation(summary = "Remove listing from favorites", description = "Removes a milk listing from current user's favorites.")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        milkFavoriteService.removeFavorite(listingId, currentUser);
        return ResponseEntity.ok(ApiResponse.messageOnly("Listing removed from favorites"));
    }

    @GetMapping
    @Operation(summary = "Get user's favorite listings", description = "Returns paginated list of favorites saved by the authenticated user.")
    public ResponseEntity<ApiResponse<PagedResponse<MilkFavoriteResponse>>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        PagedResponse<MilkFavoriteResponse> response = milkFavoriteService.getMyFavorites(pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
