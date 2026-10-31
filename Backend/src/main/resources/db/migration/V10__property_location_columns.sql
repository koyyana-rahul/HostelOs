-- V10: Add location/address columns to properties table
-- Migration adds latitude, longitude, placeId, and locationProvider columns
-- Columns are nullable to preserve existing property records
-- Decimal precision matches existing listings table pattern (9,6)

ALTER TABLE properties
    ADD COLUMN latitude DECIMAL(9, 6),

    ADD COLUMN longitude DECIMAL(9, 6),

    ADD COLUMN place_id VARCHAR(64),

    ADD COLUMN location_provider VARCHAR(50) DEFAULT 'openstreetmap';

-- Indexes for location-based queries
CREATE INDEX idx_properties_latitude ON properties (latitude);
CREATE INDEX idx_properties_longitude ON properties (longitude);
CREATE INDEX idx_properties_location_provider ON properties (location_provider);

-- Constraints to validate coordinate ranges when present
ALTER TABLE properties
    ADD CONSTRAINT chk_properties_latitude CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90));

ALTER TABLE properties
    ADD CONSTRAINT chk_properties_longitude CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180));
