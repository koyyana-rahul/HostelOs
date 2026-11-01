ALTER TABLE property_media
    ADD COLUMN upload_expires_at TIMESTAMP(6);

CREATE INDEX idx_property_media_upload_expiration
    ON property_media (status, upload_expires_at);
