package com.pashumandi.service;

import com.pashumandi.dto.image.CloudinaryUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    /**
     * Uploads a multipart image file to the designated Cloudinary folder.
     *
     * @param file   the multipart image file
     * @param folder target folder in Cloudinary (e.g., "pashumandi/profiles" or "pashumandi/animals")
     * @return CloudinaryUploadResult containing secure URL and public ID
     */
    CloudinaryUploadResult uploadImage(MultipartFile file, String folder);

    /**
     * Deletes an image asset from Cloudinary using its public ID.
     *
     * @param publicId the Cloudinary public ID
     * @return true if deletion succeeded, false otherwise
     */
    boolean deleteImage(String publicId);
}
