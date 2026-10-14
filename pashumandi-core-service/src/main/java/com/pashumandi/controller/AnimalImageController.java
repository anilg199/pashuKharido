package com.pashumandi.controller;

import com.pashumandi.dto.common.ApiResponse;
import com.pashumandi.dto.image.AnimalImageResponse;
import com.pashumandi.enums.UserRole;
import com.pashumandi.security.UserPrincipal;
import com.pashumandi.service.AnimalImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/animals")
@RequiredArgsConstructor
@Tag(name = "Animal Image Management", description = "Endpoints for uploading, listing, reordering, and deleting animal listing images")
public class AnimalImageController {

    private final AnimalImageService animalImageService;

    @PostMapping(value = "/{animalId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Upload images for animal listing",
            description = "Uploads 1 to 10 photos (JPEG, PNG, WebP, max 5 MB each) to Cloudinary. Enforces a maximum total of 10 images per animal. Only the listing owner or ADMIN can upload."
    )
    public ResponseEntity<ApiResponse<List<AnimalImageResponse>>> uploadImages(
            @Parameter(description = "ID of the animal listing", required = true)
            @PathVariable Long animalId,
            @Parameter(description = "Array of image files (JPEG, PNG, WebP)", required = true)
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal.getRole() == UserRole.ADMIN;
        List<AnimalImageResponse> responses = animalImageService.uploadImages(animalId, files, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Images uploaded successfully", responses));
    }

    @GetMapping("/{animalId}/images")
    @Operation(
            summary = "List images for an animal listing",
            description = "Retrieves all images associated with an animal ordered by display order. Publicly visible if listing is published; unpublished listings require owner or ADMIN authentication."
    )
    public ResponseEntity<ApiResponse<List<AnimalImageResponse>>> getImages(
            @Parameter(description = "ID of the animal listing", required = true)
            @PathVariable Long animalId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        boolean isAdmin = principal != null && principal.getRole() == UserRole.ADMIN;
        List<AnimalImageResponse> responses = animalImageService.getImages(animalId, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @DeleteMapping("/{animalId}/images/{imageId}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Delete an animal listing image",
            description = "Deletes the specified image from Cloudinary and database. Automatically reorders remaining images to preserve contiguous display order and updates the primary thumbnail."
    )
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @Parameter(description = "ID of the animal listing", required = true)
            @PathVariable Long animalId,
            @Parameter(description = "ID of the image to delete", required = true)
            @PathVariable Long imageId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal.getRole() == UserRole.ADMIN;
        animalImageService.deleteImage(animalId, imageId, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.messageOnly("Image deleted successfully"));
    }

    @PatchMapping("/{animalId}/images/{imageId}/primary")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Set primary listing thumbnail",
            description = "Promotes the selected image to displayOrder = 0 (primary thumbnail) and reorders subsequent images deterministically. Requires listing ownership or ADMIN authority."
    )
    public ResponseEntity<ApiResponse<List<AnimalImageResponse>>> setPrimaryImage(
            @Parameter(description = "ID of the animal listing", required = true)
            @PathVariable Long animalId,
            @Parameter(description = "ID of the image to designate as primary", required = true)
            @PathVariable Long imageId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal.getRole() == UserRole.ADMIN;
        List<AnimalImageResponse> responses = animalImageService.setPrimaryImage(animalId, imageId, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Primary image updated successfully", responses));
    }
}
