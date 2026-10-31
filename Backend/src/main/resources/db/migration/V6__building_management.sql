-- V6: Building management foundation
-- Depends on: V5 (properties table)

CREATE TABLE buildings (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    property_id BIGINT        NOT NULL,
    name        VARCHAR(120)  NOT NULL,
    code        VARCHAR(30)   NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_buildings             PRIMARY KEY (id),
    CONSTRAINT fk_buildings_property    FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE RESTRICT,
    CONSTRAINT uk_buildings_prop_code   UNIQUE (property_id, code),
    CONSTRAINT chk_buildings_status     CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_buildings_code       CHECK (code = UPPER(TRIM(code))),
    CONSTRAINT chk_buildings_code_len   CHECK (CHAR_LENGTH(code) BETWEEN 2 AND 30),
    CONSTRAINT chk_buildings_name_len   CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 120)
);

CREATE INDEX idx_buildings_property_id ON buildings (property_id);
