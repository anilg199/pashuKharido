package com.pashumandi.service;

import com.pashumandi.exception.ImageValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageValidationServiceTest {

    private ImageValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ImageValidationService(5 * 1024 * 1024, List.of("image/jpeg", "image/png", "image/webp"));
    }

    private byte[] createValidPngBytes() {
        try {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createValidJpegBytes() {
        try {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpeg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should successfully validate a valid PNG image")
    void shouldValidateValidPng() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", createValidPngBytes());
        assertDoesNotThrow(() -> validationService.validateImage(file));
    }

    @Test
    @DisplayName("Should successfully validate a valid JPEG image")
    void shouldValidateValidJpeg() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", createValidJpegBytes());
        assertDoesNotThrow(() -> validationService.validateImage(file));
    }

    @Test
    @DisplayName("Should reject null or empty file")
    void shouldRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        ImageValidationException ex = assertThrows(ImageValidationException.class, () -> validationService.validateImage(emptyFile));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Should reject oversized image (> 5 MB)")
    void shouldRejectOversizedImage() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeBytes);
        ImageValidationException ex = assertThrows(ImageValidationException.class, () -> validationService.validateImage(largeFile));
        assertTrue(ex.getMessage().contains("exceeds the maximum allowed limit"));
    }

    @Test
    @DisplayName("Should reject disallowed content type (e.g., text/plain)")
    void shouldRejectDisallowedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", "Hello World".getBytes(StandardCharsets.UTF_8));
        ImageValidationException ex = assertThrows(ImageValidationException.class, () -> validationService.validateImage(file));
        assertTrue(ex.getMessage().contains("Unsupported content type"));
    }

    @Test
    @DisplayName("Should reject file whose content is corrupt or does not match magic bytes")
    void shouldRejectSpoofedMimeType() {
        // Declared as image/jpeg, but body is plain text
        MockMultipartFile spoofedFile = new MockMultipartFile("file", "fake.jpg", "image/jpeg", "NOT_AN_IMAGE_HEADER_AT_ALL".getBytes(StandardCharsets.UTF_8));
        ImageValidationException ex = assertThrows(ImageValidationException.class, () -> validationService.validateImage(spoofedFile));
        assertTrue(ex.getMessage().contains("File content is not a recognized or permitted image format"));
    }

    @Test
    @DisplayName("Should reject file when declared format does not match actual detected format")
    void shouldRejectMismatchedFormat() {
        // PNG bytes declared as image/jpeg
        MockMultipartFile file = new MockMultipartFile("file", "mismatch.jpg", "image/jpeg", createValidPngBytes());
        ImageValidationException ex = assertThrows(ImageValidationException.class, () -> validationService.validateImage(file));
        assertTrue(ex.getMessage().contains("Mismatched image format"));
    }
}
