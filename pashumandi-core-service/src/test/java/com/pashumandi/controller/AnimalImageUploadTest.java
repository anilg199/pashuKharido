package com.pashumandi.controller;

import com.pashumandi.dto.image.CloudinaryUploadResult;
import com.pashumandi.entity.Animal;
import com.pashumandi.entity.AnimalImage;
import com.pashumandi.entity.User;
import com.pashumandi.enums.AnimalStatus;
import com.pashumandi.enums.UserRole;
import com.pashumandi.enums.UserStatus;
import com.pashumandi.exception.StorageServiceException;
import com.pashumandi.repository.AnimalImageRepository;
import com.pashumandi.repository.AnimalRepository;
import com.pashumandi.repository.UserRepository;
import com.pashumandi.security.JwtService;
import com.pashumandi.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AnimalImageUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private AnimalImageRepository animalImageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ImageStorageService imageStorageService;

    private User ownerUser;
    private User otherUser;
    private String ownerToken;
    private String otherToken;
    private Animal animal;

    @BeforeEach
    void setUp() {
        animalImageRepository.deleteAll();
        animalRepository.deleteAll();
        userRepository.deleteAll();

        ownerUser = User.builder()
                .firstName("Kisan")
                .lastName("Singh")
                .phone("9876500001")
                .email("kisan@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.SELLER)
                .status(UserStatus.ACTIVE)
                .state("Haryana")
                .district("Karnal")
                .village("Taraori")
                .pincode("132116")
                .build();
        ownerUser = userRepository.save(ownerUser);
        ownerToken = jwtService.generateAccessToken(ownerUser);

        otherUser = User.builder()
                .firstName("Other")
                .lastName("Buyer")
                .phone("9876500002")
                .email("other@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .state("Punjab")
                .district("Ludhiana")
                .village("Samrala")
                .pincode("141114")
                .build();
        otherUser = userRepository.save(otherUser);
        otherToken = jwtService.generateAccessToken(otherUser);

        animal = Animal.builder()
                .user(ownerUser)
                .title("Murrah Buffalo (First Lactation)")
                .category("BUFFALO")
                .status(AnimalStatus.PUBLISHED)
                .build();
        animal = animalRepository.save(animal);
    }

    private byte[] createValidPngBytes() {
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("1. Upload single animal image successfully")
    void shouldUploadSingleAnimalImage() throws Exception {
        byte[] imgBytes = createValidPngBytes();
        MockMultipartFile file = new MockMultipartFile("files", "cow1.png", "image/png", imgBytes);

        when(imageStorageService.uploadImage(any(), eq("pashumandi/animals")))
                .thenReturn(CloudinaryUploadResult.builder()
                        .secureUrl("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/animals/cow1.png")
                        .publicId("pashumandi/animals/cow1")
                        .format("png")
                        .bytes((long) imgBytes.length)
                        .build());

        mockMvc.perform(multipart("/api/v1/animals/{animalId}/images", animal.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].imageUrl", is("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/animals/cow1.png")))
                .andExpect(jsonPath("$.data[0].displayOrder", is(0)));

        Animal updatedAnimal = animalRepository.findById(animal.getId()).orElseThrow();
        assertEquals("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/animals/cow1.png", updatedAnimal.getPrimaryImageUrl());
    }

    @Test
    @DisplayName("2. Upload multiple animal images in a single batch")
    void shouldUploadMultipleAnimalImages() throws Exception {
        byte[] imgBytes = createValidPngBytes();
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.png", "image/png", imgBytes);
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.png", "image/png", imgBytes);

        when(imageStorageService.uploadImage(any(), eq("pashumandi/animals")))
                .thenReturn(CloudinaryUploadResult.builder()
                        .secureUrl("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/animals/img1.png")
                        .publicId("pashumandi/animals/img1")
                        .format("png")
                        .bytes((long) imgBytes.length)
                        .build())
                .thenReturn(CloudinaryUploadResult.builder()
                        .secureUrl("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/animals/img2.png")
                        .publicId("pashumandi/animals/img2")
                        .format("png")
                        .bytes((long) imgBytes.length)
                        .build());

        mockMvc.perform(multipart("/api/v1/animals/{animalId}/images", animal.getId())
                        .file(file1)
                        .file(file2)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].displayOrder", is(0)))
                .andExpect(jsonPath("$.data[1].displayOrder", is(1)));

        assertEquals(2, animalImageRepository.countByAnimalId(animal.getId()));
    }

    @Test
    @DisplayName("3. Retrieve images ordered by displayOrder")
    void shouldRetrieveImagesInDisplayOrder() throws Exception {
        AnimalImage img2 = AnimalImage.builder()
                .animal(animal)
                .imageUrl("https://res.cloudinary.com/test/img2.png")
                .cloudinaryPublicId("pashumandi/animals/img2")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .build();
        AnimalImage img1 = AnimalImage.builder()
                .animal(animal)
                .imageUrl("https://res.cloudinary.com/test/img1.png")
                .cloudinaryPublicId("pashumandi/animals/img1")
                .displayOrder(0)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
        animalImageRepository.save(img2);
        animalImageRepository.save(img1);

        mockMvc.perform(get("/api/v1/animals/{animalId}/images", animal.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].imageUrl", is("https://res.cloudinary.com/test/img1.png")))
                .andExpect(jsonPath("$.data[0].displayOrder", is(0)))
                .andExpect(jsonPath("$.data[1].imageUrl", is("https://res.cloudinary.com/test/img2.png")))
                .andExpect(jsonPath("$.data[1].displayOrder", is(1)));
    }

    @Test
    @DisplayName("4. Set primary image sets displayOrder = 0 and updates listing thumbnail")
    void shouldSetPrimaryImageSuccessfully() throws Exception {
        AnimalImage img1 = animalImageRepository.save(AnimalImage.builder()
                .animal(animal)
                .imageUrl("https://res.cloudinary.com/test/img1.png")
                .cloudinaryPublicId("pashumandi/animals/img1")
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .build());
        AnimalImage img2 = animalImageRepository.save(AnimalImage.builder()
                .animal(animal)
                .imageUrl("https://res.cloudinary.com/test/img2.png")
                .cloudinaryPublicId("pashumandi/animals/img2")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .build());

        animal.setPrimaryImageUrl(img1.getImageUrl());
        animalRepository.save(animal);

        mockMvc.perform(patch("/api/v1/animals/{animalId}/images/{imageId}/primary", animal.getId(), img2.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].id", is(img2.getId().intValue())))
                .andExpect(jsonPath("$.data[0].displayOrder", is(0)))
                .andExpect(jsonPath("$.data[1].id", is(img1.getId().intValue())))
                .andExpect(jsonPath("$.data[1].displayOrder", is(1)));

        Animal updated = animalRepository.findById(animal.getId()).orElseThrow();
        assertEquals("https://res.cloudinary.com/test/img2.png", updated.getPrimaryImageUrl());
    }

    @Test
    @DisplayName("5. Delete image reorders remaining images and updates thumbnail")
    void shouldDeleteImageAndReorderRemaining() throws Exception {
        AnimalImage img1 = animalImageRepository.save(AnimalImage.builder()
                .animal(animal)
                .imageUrl("https://res.cloudinary.com/test/img1.png")
                .cloudinaryPublicId("pashumandi/animals/img1")
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .build());
        AnimalImage img2 = animalImageRepository.save(AnimalImage.builder()
                .animal(animal)
                .imageUrl("https://res.cloudinary.com/test/img2.png")
                .cloudinaryPublicId("pashumandi/animals/img2")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .build());

        when(imageStorageService.deleteImage("pashumandi/animals/img1")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/animals/{animalId}/images/{imageId}", animal.getId(), img1.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertEquals(1, animalImageRepository.countByAnimalId(animal.getId()));
        AnimalImage remaining = animalImageRepository.findAll().get(0);
        assertEquals(img2.getId(), remaining.getId());
        assertEquals(0, remaining.getDisplayOrder());

        Animal updated = animalRepository.findById(animal.getId()).orElseThrow();
        assertEquals(img2.getImageUrl(), updated.getPrimaryImageUrl());
    }

    @Test
    @DisplayName("6. Reject uploads to animal owned by another user (403 Forbidden)")
    void shouldRejectUploadByNonOwner() throws Exception {
        byte[] imgBytes = createValidPngBytes();
        MockMultipartFile file = new MockMultipartFile("files", "img.png", "image/png", imgBytes);

        mockMvc.perform(multipart("/api/v1/animals/{animalId}/images", animal.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN_OPERATION")));
    }

    @Test
    @DisplayName("7. Reject deletion of image by non-owner (403 Forbidden)")
    void shouldRejectDeletionByNonOwner() throws Exception {
        AnimalImage img1 = animalImageRepository.save(AnimalImage.builder()
                .animal(animal)
                .imageUrl("https://res.cloudinary.com/test/img1.png")
                .cloudinaryPublicId("pashumandi/animals/img1")
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(delete("/api/v1/animals/{animalId}/images/{imageId}", animal.getId(), img1.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN_OPERATION")));
    }

    @Test
    @DisplayName("8. Reject image ID that belongs to a different animal (404 Not Found)")
    void shouldRejectImageBelongingToDifferentAnimal() throws Exception {
        Animal anotherAnimal = animalRepository.save(Animal.builder()
                .user(ownerUser)
                .title("Another Cow")
                .category("COW")
                .status(AnimalStatus.PUBLISHED)
                .build());

        AnimalImage foreignImage = animalImageRepository.save(AnimalImage.builder()
                .animal(anotherAnimal)
                .imageUrl("https://res.cloudinary.com/test/foreign.png")
                .cloudinaryPublicId("pashumandi/animals/foreign")
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(delete("/api/v1/animals/{animalId}/images/{imageId}", animal.getId(), foreignImage.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("9. Enforce 10-image limit when uploading exceeds threshold")
    void shouldRejectUploadExceeding10ImageLimit() throws Exception {
        // Pre-create 9 images
        for (int i = 0; i < 9; i++) {
            animalImageRepository.save(AnimalImage.builder()
                    .animal(animal)
                    .imageUrl("https://res.cloudinary.com/test/img" + i + ".png")
                    .cloudinaryPublicId("pashumandi/animals/img" + i)
                    .displayOrder(i)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        byte[] imgBytes = createValidPngBytes();
        MockMultipartFile file1 = new MockMultipartFile("files", "new1.png", "image/png", imgBytes);
        MockMultipartFile file2 = new MockMultipartFile("files", "new2.png", "image/png", imgBytes);

        // 9 + 2 = 11 > 10, must be rejected
        mockMvc.perform(multipart("/api/v1/animals/{animalId}/images", animal.getId())
                        .file(file1)
                        .file(file2)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("MAX_IMAGE_LIMIT_EXCEEDED")));

        assertEquals(9, animalImageRepository.countByAnimalId(animal.getId()));
        verify(imageStorageService, never()).uploadImage(any(), any());
    }

    @Test
    @DisplayName("10. Reject entire batch if one file in batch is invalid")
    void shouldRejectBatchIfOneFileIsInvalid() throws Exception {
        byte[] validBytes = createValidPngBytes();
        MockMultipartFile validFile = new MockMultipartFile("files", "valid.png", "image/png", validBytes);
        MockMultipartFile invalidFile = new MockMultipartFile("files", "corrupt.png", "image/png", "NOT_A_PNG".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/animals/{animalId}/images", animal.getId())
                        .file(validFile)
                        .file(invalidFile)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_IMAGE")));

        // Zero images uploaded
        assertEquals(0, animalImageRepository.countByAnimalId(animal.getId()));
        verify(imageStorageService, never()).uploadImage(any(), any());
    }

    @Test
    @DisplayName("11. Clean up new Cloudinary assets if upload batch encounters error")
    void shouldCleanUpCloudAssetsOnBatchFailure() throws Exception {
        byte[] imgBytes = createValidPngBytes();
        MockMultipartFile file1 = new MockMultipartFile("files", "file1.png", "image/png", imgBytes);
        MockMultipartFile file2 = new MockMultipartFile("files", "file2.png", "image/png", imgBytes);

        when(imageStorageService.uploadImage(any(), eq("pashumandi/animals")))
                .thenReturn(CloudinaryUploadResult.builder()
                        .secureUrl("https://res.cloudinary.com/xtg7v6vt/img1.png")
                        .publicId("pashumandi/animals/img1")
                        .format("png")
                        .bytes((long) imgBytes.length)
                        .build())
                .thenThrow(new StorageServiceException("Network error on second upload"));

        mockMvc.perform(multipart("/api/v1/animals/{animalId}/images", animal.getId())
                        .file(file1)
                        .file(file2)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode", is("STORAGE_SERVICE_ERROR")));

        // Verify cleanup of first uploaded asset
        verify(imageStorageService, times(1)).deleteImage("pashumandi/animals/img1");
        assertEquals(0, animalImageRepository.countByAnimalId(animal.getId()));
    }

    @Test
    @DisplayName("12. Restrict public access to unpublished draft listing images")
    void shouldRestrictAccessToDraftListingImages() throws Exception {
        animal.setStatus(AnimalStatus.DRAFT);
        animalRepository.save(animal);

        animalImageRepository.save(AnimalImage.builder()
                .animal(animal)
                .imageUrl("https://res.cloudinary.com/test/draft.png")
                .cloudinaryPublicId("pashumandi/animals/draft")
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .build());

        // Anonymous public user cannot view
        mockMvc.perform(get("/api/v1/animals/{animalId}/images", animal.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN_OPERATION")));

        // Non-owner cannot view
        mockMvc.perform(get("/api/v1/animals/{animalId}/images", animal.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN_OPERATION")));

        // Owner CAN view draft listing images
        mockMvc.perform(get("/api/v1/animals/{animalId}/images", animal.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }
}
