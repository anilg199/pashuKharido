-- ==============================================================================
-- Migration V3: Standardize User Role and Status Columns to VARCHAR
-- Application: PashuMandi Enterprise Livestock Marketplace
-- Purpose: Ensures role and status columns match JPA entity mapping (VARCHAR)
--          and V1 schema specification, preserving all existing user records safely.
-- ==============================================================================

ALTER TABLE users 
    MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'BUYER',
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
