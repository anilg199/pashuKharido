-- ==============================================================================
-- Migration V4: Add profile_image_public_id column to users table
-- Application: PashuMandi Enterprise Livestock Marketplace
-- Purpose: Persists Cloudinary asset public ID for profile photos
-- ==============================================================================

ALTER TABLE users
    ADD COLUMN profile_image_public_id VARCHAR(255) NULL AFTER profile_image_url;
