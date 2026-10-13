package com.pashumandi.service;

import com.pashumandi.dto.image.AnimalImageResponse;
import com.pashumandi.dto.image.CloudinaryUploadResult;
import com.pashumandi.entity.Animal;
import com.pashumandi.entity.AnimalImage;
import com.pashumandi.enums.AnimalStatus;
import com.pashumandi.exception.MaxImageLimitExceededException;
import com.pashumandi.exception.ResourceNotFoundException;
import com.pashumandi.exception.StorageServiceException;
import com.pashumandi.exception.UnauthorizedAccessException;
import com.pashumandi.repository.AnimalImageRepository;
import com.pashumandi.repository.AnimalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalImageService {

    public static final int MAX_IMAGES_PER_ANIMAL = 10;
    private static final String ANIMAL_FOLDER = "pashumandi/animals";

    private final AnimalRepository animalRepository;
    private final AnimalImageRepository animalImageRepository;
    private final ImageStorageService imageStorageService;
    private final ImageValidationService imageValidationService;

    /**
     * Uploads a batch of images for an animal listing.
     * Transaction-safe with pessimistic locking on the animal record and all-or-nothing cloud cleanup.
     */
    @Transactional
    public List<AnimalImageResponse> uploadImages(Long animalId, List<MultipartFile> files, Long authenticatedUserId, boolean isAdmin) {
        if (files == null || files.isEmpty()) {
            throw new StorageServiceException("At least one image file must be provided for upload");
        }

        // 1. Validate every file before uploading any to Cloudinary
        for (MultipartFile file : files) {
            imageValidationService.validateImage(file);
        }

        // 2. Lock animal record to prevent concurrent upload races that could bypass the 10-image limit
        Animal animal = animalRepository.findByIdForUpdate(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + animalId));

        // 3. Verify ownership
        verifyOwnership(animal, authenticatedUserId, isAdmin);

        // 4. Enforce 10-image limit across existing and requested files
        long existingCount = animalImageRepository.countByAnimalId(animalId);
        if (existingCount + files.size() > MAX_IMAGES_PER_ANIMAL) {
            throw new MaxImageLimitExceededException(String.format(
                    "Upload rejected: Animal currently has %d images. Adding %d images would exceed the maximum limit of %d.",
                    existingCount, files.size(), MAX_IMAGES_PER_ANIMAL
            ));
        }

        // 5. Upload files to Cloudinary with all-or-nothing batch tracking
        List<CloudinaryUploadResult> newCloudAssets = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                CloudinaryUploadResult uploadResult = imageStorageService.uploadImage(file, ANIMAL_FOLDER);
                newCloudAssets.add(uploadResult);
            }
        } catch (Exception ex) {
            log.error("Cloudinary batch upload failed for animal ID: {}. Cleaning up {} uploaded assets.", animalId, newCloudAssets.size());
            for (CloudinaryUploadResult uploaded : newCloudAssets) {
                imageStorageService.deleteImage(uploaded.getPublicId());
            }
            throw new StorageServiceException("Failed to upload image batch to cloud storage: " + ex.getMessage(), ex);
        }

        // 6. Register post-rollback cleanup hook in case database commit fails
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        log.warn("Database transaction rolled back: cleaning up {} uploaded assets for animal ID: {}", newCloudAssets.size(), animalId);
                        for (CloudinaryUploadResult asset : newCloudAssets) {
                            imageStorageService.deleteImage(asset.getPublicId());
                        }
                    }
                }
            });
        }

        // 7. Persist image records in deterministic display order
        int currentMaxOrder = animalImageRepository.findMaxDisplayOrderByAnimalId(animalId);
        int nextOrder = currentMaxOrder < 0 ? 0 : currentMaxOrder + 1;

        List<AnimalImage> savedImages = new ArrayList<>();
        try {
            for (CloudinaryUploadResult uploadResult : newCloudAssets) {
                AnimalImage image = AnimalImage.builder()
                        .animal(animal)
                        .imageUrl(uploadResult.getSecureUrl())
                        .cloudinaryPublicId(uploadResult.getPublicId())
                        .displayOrder(nextOrder++)
                        .createdAt(LocalDateTime.now())
                        .build();

                savedImages.add(animalImageRepository.save(image));
            }

            // If animal has no primary thumbnail yet, set to first image
            if (!StringUtils.hasText(animal.getPrimaryImageUrl()) && !savedImages.isEmpty()) {
                animal.setPrimaryImageUrl(savedImages.get(0).getImageUrl());
                animalRepository.save(animal);
            }

            log.info("Successfully persisted {} images for animal ID: {}", savedImages.size(), animalId);

        } catch (Exception ex) {
            log.error("Failed to persist animal image records in database. Cleaning up cloud assets for animal ID: {}", animalId, ex);
            for (CloudinaryUploadResult asset : newCloudAssets) {
                imageStorageService.deleteImage(asset.getPublicId());
            }
            throw ex;
        }

        return savedImages.stream()
                .map(AnimalImageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lists images for an animal listing.
     * Public users can read images only if listing is PUBLISHED.
     * Unpublished listings require ownership or ADMIN authority.
     */
    @Transactional(readOnly = true)
    public List<AnimalImageResponse> getImages(Long animalId, Long authenticatedUserId, boolean isAdmin) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + animalId));

        if (animal.getStatus() != AnimalStatus.PUBLISHED) {
            if (authenticatedUserId == null) {
                throw new UnauthorizedAccessException("Listing is not published. Authentication is required to view images.");
            }
            verifyOwnership(animal, authenticatedUserId, isAdmin);
        }

        return animalImageRepository.findByAnimalIdOrderByDisplayOrderAscIdAsc(animalId).stream()
                .map(AnimalImageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Deletes an animal image, reorders remaining images contiguously, and updates the listing thumbnail.
     */
    @Transactional
    public void deleteImage(Long animalId, Long imageId, Long authenticatedUserId, boolean isAdmin) {
        Animal animal = animalRepository.findByIdForUpdate(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + animalId));

        verifyOwnership(animal, authenticatedUserId, isAdmin);

        AnimalImage imageToDelete = animalImageRepository.findByAnimalIdAndId(animalId, imageId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Image %d not found for animal %d", imageId, animalId)));

        String publicIdToDelete = imageToDelete.getCloudinaryPublicId();

        // Remove from database
        animalImageRepository.delete(imageToDelete);
        animalImageRepository.flush();

        // Reorder remaining images contiguously from displayOrder = 0
        List<AnimalImage> remaining = animalImageRepository.findByAnimalIdOrderByDisplayOrderAscIdAsc(animalId);
        for (int i = 0; i < remaining.size(); i++) {
            AnimalImage current = remaining.get(i);
            if (!current.getDisplayOrder().equals(i)) {
                current.setDisplayOrder(i);
                animalImageRepository.save(current);
            }
        }

        // Update listing thumbnail
        if (remaining.isEmpty()) {
            animal.setPrimaryImageUrl(null);
        } else {
            animal.setPrimaryImageUrl(remaining.get(0).getImageUrl());
        }
        animalRepository.save(animal);

        // Delete cloud asset after successful database commit
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Post-commit: deleting Cloudinary asset for animal image ID: {} (publicId: {})", imageId, publicIdToDelete);
                    imageStorageService.deleteImage(publicIdToDelete);
                }
            });
        }

        log.info("Successfully deleted animal image {} and reordered remaining {} images", imageId, remaining.size());
    }

    /**
     * Sets an image as the primary thumbnail (displayOrder = 0) and reorders the rest deterministically.
     */
    @Transactional
    public List<AnimalImageResponse> setPrimaryImage(Long animalId, Long imageId, Long authenticatedUserId, boolean isAdmin) {
        Animal animal = animalRepository.findByIdForUpdate(animalId)
                .orElseThrow(() -> new ResourceNotFoundException("Animal not found with id: " + animalId));

        verifyOwnership(animal, authenticatedUserId, isAdmin);

        AnimalImage primaryImage = animalImageRepository.findByAnimalIdAndId(animalId, imageId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Image %d not found for animal %d", imageId, animalId)));

        List<AnimalImage> allImages = animalImageRepository.findByAnimalIdOrderByDisplayOrderAscIdAsc(animalId);

        // Target image becomes index 0
        primaryImage.setDisplayOrder(0);
        animalImageRepository.save(primaryImage);

        // Reorder others deterministically
        int order = 1;
        for (AnimalImage image : allImages) {
            if (!image.getId().equals(imageId)) {
                image.setDisplayOrder(order++);
                animalImageRepository.save(image);
            }
        }

        animal.setPrimaryImageUrl(primaryImage.getImageUrl());
        animalRepository.save(animal);

        log.info("Set image {} as primary thumbnail for animal {}", imageId, animalId);

        return animalImageRepository.findByAnimalIdOrderByDisplayOrderAscIdAsc(animalId).stream()
                .map(AnimalImageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void verifyOwnership(Animal animal, Long authenticatedUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (authenticatedUserId == null || animal.getUser() == null || !animal.getUser().getId().equals(authenticatedUserId)) {
            throw new UnauthorizedAccessException("You are not authorized to modify images for this animal listing.");
        }
    }
}
