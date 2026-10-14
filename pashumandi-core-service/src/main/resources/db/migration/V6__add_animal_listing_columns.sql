-- ==============================================================================
-- Migration V6: Expand Animals Table with Livestock Listing Attributes
-- Application: PashuMandi Enterprise Livestock Marketplace
-- Purpose: Adds comprehensive livestock listing attributes, pricing, location,
--          and performance indexes for browsing and geospatial search.
-- ==============================================================================

ALTER TABLE animals
    ADD COLUMN description TEXT NULL AFTER title,
    ADD COLUMN animal_type VARCHAR(50) NULL AFTER description,
    ADD COLUMN breed VARCHAR(100) NULL AFTER animal_type,
    ADD COLUMN age_months INT NULL AFTER breed,
    ADD COLUMN gender VARCHAR(20) NULL AFTER age_months,
    ADD COLUMN price DECIMAL(12, 2) NULL AFTER gender,
    ADD COLUMN negotiable BOOLEAN DEFAULT FALSE AFTER price,
    ADD COLUMN weight_kg DECIMAL(8, 2) NULL AFTER primary_image_url,
    ADD COLUMN milk_yield_litres_per_day DECIMAL(6, 2) NULL AFTER weight_kg,
    ADD COLUMN lactation_status VARCHAR(50) NULL AFTER milk_yield_litres_per_day,
    ADD COLUMN pregnant BOOLEAN NULL AFTER lactation_status,
    ADD COLUMN vaccinated BOOLEAN NULL AFTER pregnant,
    ADD COLUMN health_description VARCHAR(500) NULL AFTER vaccinated,
    ADD COLUMN state VARCHAR(100) NULL AFTER health_description,
    ADD COLUMN district VARCHAR(100) NULL AFTER state,
    ADD COLUMN village VARCHAR(100) NULL AFTER district,
    ADD COLUMN pincode VARCHAR(10) NULL AFTER village,
    ADD COLUMN latitude DECIMAL(10, 7) NULL AFTER pincode,
    ADD COLUMN longitude DECIMAL(10, 7) NULL AFTER latitude;

-- Backfill animal_type from existing category column if any rows exist
UPDATE animals SET animal_type = category WHERE animal_type IS NULL AND category IS NOT NULL;

-- Allow category to be nullable now that animal_type is the primary enum
ALTER TABLE animals MODIFY COLUMN category VARCHAR(50) NULL;

-- Indexes for efficient filtering and searching
CREATE INDEX idx_animals_type ON animals(animal_type);
CREATE INDEX idx_animals_breed ON animals(breed);
CREATE INDEX idx_animals_price ON animals(price);
CREATE INDEX idx_animals_state_district ON animals(state, district);
CREATE INDEX idx_animals_created_at ON animals(created_at);
CREATE INDEX idx_animals_lat_long ON animals(latitude, longitude);
