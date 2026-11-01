CREATE TABLE property_media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    provider_type VARCHAR(30) NOT NULL,
    provider_storage_key VARCHAR(512) NOT NULL,
    provider_asset_id VARCHAR(255),
    mime_type VARCHAR(100) NOT NULL,
    format VARCHAR(20) NOT NULL,
    secure_url VARCHAR(1000),
    byte_size BIGINT NOT NULL,
    width INT,
    height INT,
    checksum_sha256 CHAR(64),
    position INT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    migration_status VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED',
    migration_source_provider VARCHAR(30),
    migration_target_provider VARCHAR(30),
    migration_source_key VARCHAR(512),
    migration_target_key VARCHAR(512),
    migration_checksum_sha256 CHAR(64),
    migration_idempotency_key VARCHAR(128),
    migration_retry_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    primary_property_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN status = 'READY' AND is_primary = TRUE THEN property_id
                ELSE NULL
            END
        ) STORED,
    CONSTRAINT pk_property_media PRIMARY KEY (id),
    CONSTRAINT fk_property_media_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_property_media_property FOREIGN KEY (property_id)
        REFERENCES properties (id) ON DELETE RESTRICT,
    CONSTRAINT uk_property_media_provider_key UNIQUE (provider_type, provider_storage_key),
    CONSTRAINT uk_property_media_primary UNIQUE (primary_property_id),
    CONSTRAINT chk_property_media_provider CHECK (provider_type IN ('CLOUDINARY', 'S3')),
    CONSTRAINT chk_property_media_status CHECK (
        status IN ('PENDING', 'READY', 'FAILED', 'DELETING', 'DELETED')
    ),
    CONSTRAINT chk_property_media_migration_status CHECK (
        migration_status IN (
            'NOT_STARTED',
            'COPYING',
            'VERIFIED',
            'CUTOVER_READY',
            'CUTOVER_COMPLETE',
            'ROLLBACK_READY',
            'FAILED'
        )
    ),
    CONSTRAINT chk_property_media_byte_size CHECK (byte_size > 0),
    CONSTRAINT chk_property_media_position CHECK (position >= 0),
    CONSTRAINT chk_property_media_dimensions CHECK (
        (width IS NULL AND height IS NULL) OR (width > 0 AND height > 0)
    ),
    CONSTRAINT chk_property_media_checksum CHECK (
        checksum_sha256 IS NULL OR checksum_sha256 REGEXP '^[0-9a-fA-F]{64}$'
    ),
    CONSTRAINT chk_property_media_migration_checksum CHECK (
        migration_checksum_sha256 IS NULL
        OR migration_checksum_sha256 REGEXP '^[0-9a-fA-F]{64}$'
    ),
    CONSTRAINT chk_property_media_retry_count CHECK (migration_retry_count >= 0)
);

CREATE INDEX idx_property_media_property_order
    ON property_media (organization_id, property_id, status, position);
CREATE INDEX idx_property_media_property_status
    ON property_media (organization_id, property_id, status);
CREATE INDEX idx_property_media_migration
    ON property_media (migration_status, migration_target_provider);
