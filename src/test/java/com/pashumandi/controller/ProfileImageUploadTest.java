package com.pashumandi.controller;

import com.pashumandi.dto.image.CloudinaryUploadResult;
import com.pashumandi.entity.User;
import com.pashumandi.enums.UserRole;
import com.pashumandi.enums.UserStatus;
import com.pashumandi.exception.StorageServiceException;
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

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProfileImageUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ImageStorageService imageStorageService;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .firstName("Ramesh")
                .lastName("Kumar")
                .phone("9811223344")
                .email("ramesh@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .state("Rajasthan")
                .district("Jaipur")
                .village("Chaksu")
                .pincode("303901")
                .build();
        testUser = userRepository.save(testUser);
        jwtToken = jwtService.generateAccessToken(testUser);
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
    @DisplayName("1. Authenticated user can upload a valid profile image successfully")
    void shouldUploadProfileImageSuccessfully() throws Exception {
        byte[] imageBytes = createValidPngBytes();
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", imageBytes);

        when(imageStorageService.uploadImage(any(), eq("pashumandi/profiles")))
                .thenReturn(CloudinaryUploadResult.builder()
                        .secureUrl("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/profiles/avatar_123.png")
                        .publicId("pashumandi/profiles/avatar_123")
                        .format("png")
                        .bytes((long) imageBytes.length)
                        .build());

        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Profile image updated successfully")))
                .andExpect(jsonPath("$.data.profileImageUrl", is("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/profiles/avatar_123.png")))
                .andExpect(jsonPath("$.data.profileImagePublicId").doesNotExist());

        // Verify updated in database
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/profiles/avatar_123.png", updatedUser.getProfileImageUrl());
        assertEquals("pashumandi/profiles/avatar_123", updatedUser.getProfileImagePublicId());
    }

    @Test
    @DisplayName("2. Profile image replacement deletes old asset only after successful save")
    void shouldReplaceProfileImageAndCleanupOldAsset() throws Exception {
        // Set existing profile image
        testUser.setProfileImageUrl("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/profiles/old_avatar.png");
        testUser.setProfileImagePublicId("pashumandi/profiles/old_avatar");
        userRepository.save(testUser);

        byte[] newImageBytes = createValidPngBytes();
        MockMultipartFile newFile = new MockMultipartFile("file", "new_avatar.png", "image/png", newImageBytes);

        when(imageStorageService.uploadImage(any(), eq("pashumandi/profiles")))
                .thenReturn(CloudinaryUploadResult.builder()
                        .secureUrl("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/profiles/new_avatar.png")
                        .publicId("pashumandi/profiles/new_avatar")
                        .format("png")
                        .bytes((long) newImageBytes.length)
                        .build());

        when(imageStorageService.deleteImage("pashumandi/profiles/old_avatar")).thenReturn(true);

        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(newFile)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.profileImageUrl", is("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/profiles/new_avatar.png")));

        // Verify old image deletion was invoked
        verify(imageStorageService, times(1)).deleteImage("pashumandi/profiles/old_avatar");

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("pashumandi/profiles/new_avatar", updatedUser.getProfileImagePublicId());
    }

    @Test
    @DisplayName("3. Delete profile image clears database reference and deletes Cloudinary asset")
    void shouldDeleteProfileImageSuccessfully() throws Exception {
        testUser.setProfileImageUrl("https://res.cloudinary.com/xtg7v6vt/image/upload/v1/pashumandi/profiles/avatar.png");
        testUser.setProfileImagePublicId("pashumandi/profiles/avatar");
        userRepository.save(testUser);

        when(imageStorageService.deleteImage("pashumandi/profiles/avatar")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/users/me/profile-image")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.profileImageUrl").doesNotExist());

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertNull(updatedUser.getProfileImageUrl());
        assertNull(updatedUser.getProfileImagePublicId());
        verify(imageStorageService, times(1)).deleteImage("pashumandi/profiles/avatar");
    }

    @Test
    @DisplayName("4. Upload without authentication is rejected with 401 Unauthorized")
    void shouldRejectUnauthenticatedUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", createValidPngBytes());

        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("5. Empty file upload is rejected with 400 Bad Request")
    void shouldRejectEmptyFileUpload() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_IMAGE")));
    }

    @Test
    @DisplayName("6. Invalid MIME type (e.g. text/plain) is rejected with 400 Bad Request")
    void shouldRejectInvalidMimeType() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(textFile)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_IMAGE")));
    }

    @Test
    @DisplayName("7. Spoofed MIME type with non-image bytes is rejected with 400 Bad Request")
    void shouldRejectSpoofedFile() throws Exception {
        MockMultipartFile spoofedFile = new MockMultipartFile("file", "fake.png", "image/png", "FAKE_PNG_CONTENT".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(spoofedFile)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_IMAGE")));
    }

    @Test
    @DisplayName("8. Oversized file (> 5 MB) is rejected")
    void shouldRejectOversizedImage() throws Exception {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(largeFile)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_IMAGE")));
    }

    @Test
    @DisplayName("9. Cloudinary upload failure returns 500 and leaves database untouched")
    void shouldHandleCloudinaryFailureGracefully() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", createValidPngBytes());

        when(imageStorageService.uploadImage(any(), eq("pashumandi/profiles")))
                .thenThrow(new StorageServiceException("Cloudinary network timeout"));

        mockMvc.perform(multipart("/api/v1/users/me/profile-image")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode", is("STORAGE_SERVICE_ERROR")));

        User unchangedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertNull(unchangedUser.getProfileImageUrl());
    }
}
