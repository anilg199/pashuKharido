-- ====================================================================
-- PashuMandi Milk Marketplace Microservice - Schema Migration V1
-- Dedicated database schema for Milk Marketplace
-- ====================================================================

-- 1. Milk Listings Table
CREATE TABLE IF NOT EXISTS milk_listings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    milk_type VARCHAR(20) NOT NULL,
    animal_source VARCHAR(100),
    price_per_litre DECIMAL(10, 2) NOT NULL,
    available_quantity_litres DECIMAL(8, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'LITRE',
    fat_percentage DECIMAL(4, 2),
    snf_percentage DECIMAL(4, 2),
    quality_description VARCHAR(500),
    state VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    village VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    fulfillment_type VARCHAR(20) NOT NULL,
    availability_start TIME,
    availability_end TIME,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    image_url VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_milk_listings_seller (seller_user_id),
    INDEX idx_milk_listings_status (status),
    INDEX idx_milk_listings_type (milk_type),
    INDEX idx_milk_listings_price (price_per_litre),
    INDEX idx_milk_listings_location (state, district, pincode),
    INDEX idx_milk_listings_fulfillment (fulfillment_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Milk Orders Table
CREATE TABLE IF NOT EXISTS milk_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(36) NOT NULL UNIQUE,
    listing_id BIGINT NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    seller_user_id BIGINT NOT NULL,
    quantity_litres DECIMAL(8, 2) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    fulfillment_type VARCHAR(20) NOT NULL,
    scheduled_for TIMESTAMP NULL,
    delivery_state VARCHAR(100),
    delivery_district VARCHAR(100),
    delivery_village VARCHAR(100),
    delivery_pincode VARCHAR(10),
    delivery_notes VARCHAR(500),
    order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    cancellation_reason VARCHAR(255),
    cancelled_by VARCHAR(20),
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_milk_orders_listing FOREIGN KEY (listing_id) REFERENCES milk_listings(id) ON DELETE RESTRICT,
    INDEX idx_milk_orders_buyer (buyer_user_id),
    INDEX idx_milk_orders_seller (seller_user_id),
    INDEX idx_milk_orders_status (order_status),
    INDEX idx_milk_orders_listing (listing_id),
    INDEX idx_milk_orders_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Milk Subscriptions Table
CREATE TABLE IF NOT EXISTS milk_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_number VARCHAR(36) NOT NULL UNIQUE,
    listing_id BIGINT NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    seller_user_id BIGINT NOT NULL,
    quantity_litres DECIMAL(8, 2) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    preferred_time_slot VARCHAR(50),
    fulfillment_type VARCHAR(20) NOT NULL,
    delivery_state VARCHAR(100),
    delivery_district VARCHAR(100),
    delivery_village VARCHAR(100),
    delivery_pincode VARCHAR(10),
    delivery_notes VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_milk_sub_listing FOREIGN KEY (listing_id) REFERENCES milk_listings(id) ON DELETE RESTRICT,
    INDEX idx_milk_sub_buyer (buyer_user_id),
    INDEX idx_milk_sub_seller (seller_user_id),
    INDEX idx_milk_sub_status (status),
    INDEX idx_milk_sub_listing (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Milk Favorites Table
CREATE TABLE IF NOT EXISTS milk_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_listing_favorite UNIQUE (user_id, listing_id),
    CONSTRAINT fk_milk_fav_listing FOREIGN KEY (listing_id) REFERENCES milk_listings(id) ON DELETE CASCADE,
    INDEX idx_milk_fav_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Milk Inquiries Table
CREATE TABLE IF NOT EXISTS milk_inquiries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    seller_user_id BIGINT NOT NULL,
    message VARCHAR(1000) NOT NULL,
    phone VARCHAR(15),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_milk_inq_listing FOREIGN KEY (listing_id) REFERENCES milk_listings(id) ON DELETE CASCADE,
    INDEX idx_milk_inq_sender (sender_user_id),
    INDEX idx_milk_inq_seller (seller_user_id),
    INDEX idx_milk_inq_listing (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
