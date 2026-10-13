package com.pashumandi.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.pashumandi.dto.image.CloudinaryUploadResult;
import com.pashumandi.exception.StorageServiceException;
import com.pashumandi.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageStorageService implements ImageStorageService {

    private final Cloudinary cloudinary;

    @Override
    public CloudinaryUploadResult uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new StorageServiceException("Cannot upload empty or null file");
        }

        String uniquePublicId = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> params = ObjectUtils.asMap(
                "folder", folder,
                "public_id", uniquePublicId,
                "overwrite", false,
                "resource_type", "image"
        );

        try {
            log.info("Uploading image to Cloudinary folder: {} with public_id: {}", folder, uniquePublicId);
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            String format = (String) uploadResult.get("format");
            Number bytes = (Number) uploadResult.get("bytes");

            if (!StringUtils.hasText(secureUrl) || !StringUtils.hasText(publicId)) {
                throw new StorageServiceException("Cloudinary upload did not return a valid secure URL or public ID");
            }

            log.info("Cloudinary upload successful. Public ID: {}", publicId);

            return CloudinaryUploadResult.builder()
                    .secureUrl(secureUrl)
                    .publicId(publicId)
                    .format(format)
                    .bytes(bytes != null ? bytes.longValue() : file.getSize())
                    .build();

        } catch (IOException ex) {
            log.error("Failed to read image bytes or upload to Cloudinary: {}", ex.getMessage());
            throw new StorageServiceException("Failed to upload image to cloud storage: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Cloudinary upload error: {}", ex.getMessage());
            throw new StorageServiceException("Cloud storage service encountered an error during upload: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteImage(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            log.debug("No Cloudinary public ID provided for deletion");
            return false;
        }

        try {
            log.info("Attempting to delete Cloudinary asset with public ID: {}", publicId);
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String resultStatus = (String) result.get("result");

            if ("ok".equalsIgnoreCase(resultStatus)) {
                log.info("Cloudinary asset successfully deleted: {}", publicId);
                return true;
            } else if ("not found".equalsIgnoreCase(resultStatus)) {
                log.warn("Cloudinary asset was not found: {}", publicId);
                return false;
            } else {
                log.warn("Cloudinary asset deletion returned status '{}' for: {}", resultStatus, publicId);
                return false;
            }
        } catch (Exception ex) {
            log.warn("Operational warning: Failed to delete Cloudinary asset with public ID '{}': {}", publicId, ex.getMessage());
            return false;
        }
    }
}
