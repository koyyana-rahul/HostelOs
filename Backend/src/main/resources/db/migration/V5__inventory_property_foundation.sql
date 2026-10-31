CREATE TABLE properties (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) NOT NULL DEFAULT 'IN',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_properties PRIMARY KEY (id),
    CONSTRAINT fk_properties_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
    CONSTRAINT uk_properties_org_code UNIQUE (organization_id, code),
    CONSTRAINT uk_properties_tenant_id UNIQUE (organization_id, id),
    CONSTRAINT chk_properties_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_properties_code CHECK (code = UPPER(TRIM(code))),
    CONSTRAINT chk_properties_code_length CHECK (CHAR_LENGTH(code) BETWEEN 2 AND 30),
    CONSTRAINT chk_properties_name_length CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 120),
    CONSTRAINT chk_properties_city_length CHECK (CHAR_LENGTH(TRIM(city)) BETWEEN 1 AND 100)
);

CREATE INDEX idx_properties_organization_id ON properties (organization_id);
CREATE INDEX idx_properties_org_status ON properties (organization_id, status);
