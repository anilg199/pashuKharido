package com.pashumandi.service;

import com.pashumandi.exception.ImageValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class ImageValidationService {

    private static final long DEFAULT_MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final long maxFileSize;
    private final List<String> allowedContentTypes;

    public ImageValidationService(
            @Value("${app.image-upload.max-file-size:5242880}") long maxFileSize,
            @Value("${app.image-upload.allowed-content-types:image/jpeg,image/png,image/webp}") List<String> allowedContentTypes
    ) {
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
        this.allowedContentTypes = allowedContentTypes != null && !allowedContentTypes.isEmpty()
                ? allowedContentTypes
                : Arrays.asList("image/jpeg", "image/png", "image/webp");
    }

    /**
     * Validates an uploaded image for presence, size, MIME type, magic bytes, and decode integrity.
     *
     * @param file the multipart file to validate
     */
    public void validateImage(MultipartFile file) {
        // 1. Reject null or empty file
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new ImageValidationException("Image file cannot be empty. Please select a valid image.");
        }

        // 2. Reject oversized file
        if (file.getSize() > maxFileSize) {
            throw new ImageValidationException(String.format(
                    "Image file size (%d bytes) exceeds the maximum allowed limit of %d bytes (5 MB).",
                    file.getSize(), maxFileSize
            ));
        }

        // 3. Check declared content type as initial screen
        String declaredContentType = file.getContentType();
        if (!StringUtils.hasText(declaredContentType) || !isDeclaredTypeAllowed(declaredContentType.trim().toLowerCase())) {
            throw new ImageValidationException(String.format(
                    "Unsupported content type '%s'. Only JPEG, PNG, and WebP images are allowed.",
                    declaredContentType != null ? declaredContentType : "unknown"
            ));
        }

        // 4. Read header / magic bytes
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read image bytes: {}", e.getMessage());
            throw new ImageValidationException("Failed to read image data. The file may be unreadable.");
        }

        if (fileBytes.length < 12) {
            throw new ImageValidationException("Image file is too small or corrupt to be a valid image.");
        }

        String detectedFormat = detectActualImageFormat(fileBytes);
        if (detectedFormat == null) {
            throw new ImageValidationException("File content is not a recognized or permitted image format (JPEG, PNG, WebP).");
        }

        // 5. Verify declared content type matches actual detected format
        validateFormatConsistency(declaredContentType.trim().toLowerCase(), detectedFormat);

        // 6. Deep inspection: verify image can be decoded without corruption
        verifyImageIntegrity(fileBytes, detectedFormat);
    }

    private boolean isDeclaredTypeAllowed(String contentType) {
        return allowedContentTypes.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(contentType));
    }

    /**
     * Detects image format by inspecting magic bytes without relying on file extension or client header.
     */
    public String detectActualImageFormat(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }

        // JPEG: Starts with 0xFF, 0xD8, 0xFF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "JPEG";
        }

        // PNG: Starts with 0x89, 0x50 ('P'), 0x4E ('N'), 0x47 ('G'), 0x0D, 0x0A, 0x1A, 0x0A
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "PNG";
        }

        // WebP: RIFF (bytes 0-3) + file length (bytes 4-7) + WEBP (bytes 8-11)
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "WEBP";
        }

        return null;
    }

    private void validateFormatConsistency(String declaredType, String detectedFormat) {
        boolean match = switch (detectedFormat) {
            case "JPEG" -> declaredType.equals("image/jpeg") || declaredType.equals("image/jpg");
            case "PNG" -> declaredType.equals("image/png");
            case "WEBP" -> declaredType.equals("image/webp");
            default -> false;
        };

        if (!match) {
            throw new ImageValidationException(String.format(
                    "Mismatched image format: declared '%s' but actual file content is '%s'.",
                    declaredType, detectedFormat
            ));
        }
    }

    private void verifyImageIntegrity(byte[] bytes, String detectedFormat) {
        if ("JPEG".equals(detectedFormat) || "PNG".equals(detectedFormat)) {
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                BufferedImage image = ImageIO.read(is);
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new ImageValidationException("Image data is corrupted or cannot be decoded.");
                }
            } catch (ImageValidationException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Image decode failure for format {}: {}", detectedFormat, e.getMessage());
                throw new ImageValidationException("Image file appears to be corrupted and cannot be safely processed.");
            }
        } else if ("WEBP".equals(detectedFormat)) {
            // Validate WebP RIFF chunk header structure
            if (bytes.length < 16) {
                throw new ImageValidationException("Corrupted WebP image file: header incomplete.");
            }
            // Bytes 12-15 must start with VP8, VP8L, or VP8X
            String chunkHeader = new String(bytes, 12, 4);
            if (!chunkHeader.startsWith("VP8")) {
                throw new ImageValidationException("Corrupted or unsupported WebP image format.");
            }
        }
    }
}
