CREATE TABLE listings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    property_id BIGINT,
    title VARCHAR(160) NOT NULL,
    title_key VARCHAR(160) NOT NULL,
    description VARCHAR(4000),
    accommodation_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    address_line1 VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    latitude DECIMAL(9, 6),
    longitude DECIMAL(9, 6),
    base_price DECIMAL(10, 2),
    currency VARCHAR(3) NOT NULL,
    price_period VARCHAR(20) NOT NULL,
    contact_name VARCHAR(120),
    contact_email VARCHAR(254),
    contact_phone VARCHAR(40),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    archived_at TIMESTAMP(6),
    CONSTRAINT pk_listings PRIMARY KEY (id),
    CONSTRAINT fk_listings_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_listings_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE RESTRICT,
    CONSTRAINT uk_listings_org_title_key UNIQUE (organization_id, title_key),
    CONSTRAINT chk_listings_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_listings_type CHECK (accommodation_type IN ('HOSTEL', 'PG', 'APARTMENT', 'ROOM', 'OTHER')),
    CONSTRAINT chk_listings_price CHECK (base_price >= 0),
    CONSTRAINT chk_listings_currency CHECK (CHAR_LENGTH(currency) = 3),
    CONSTRAINT chk_listings_title_length CHECK (CHAR_LENGTH(TRIM(title)) BETWEEN 3 AND 160),
    CONSTRAINT chk_listings_latitude CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    CONSTRAINT chk_listings_longitude CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);

CREATE INDEX idx_listings_organization_id ON listings (organization_id);
CREATE INDEX idx_listings_org_status ON listings (organization_id, status);
CREATE INDEX idx_listings_property_id ON listings (property_id);
CREATE INDEX idx_listings_status_city ON listings (status, city);

CREATE TABLE listing_enquiries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    customer_user_id BIGINT,
    customer_name VARCHAR(120) NOT NULL,
    customer_email VARCHAR(254) NOT NULL,
    customer_phone VARCHAR(40),
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_listing_enquiries PRIMARY KEY (id),
    CONSTRAINT fk_listing_enquiries_organization FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_listing_enquiries_listing FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE RESTRICT,
    CONSTRAINT fk_listing_enquiries_customer FOREIGN KEY (customer_user_id) REFERENCES user_accounts (id) ON DELETE RESTRICT,
    CONSTRAINT chk_listing_enquiries_status CHECK (status IN ('NEW', 'READ', 'RESPONDED', 'CLOSED')),
    CONSTRAINT chk_listing_enquiries_message_length CHECK (CHAR_LENGTH(TRIM(message)) BETWEEN 1 AND 2000)
);

CREATE INDEX idx_listing_enquiries_organization_id ON listing_enquiries (organization_id);
CREATE INDEX idx_listing_enquiries_org_status ON listing_enquiries (organization_id, status);
CREATE INDEX idx_listing_enquiries_listing_id ON listing_enquiries (listing_id);
CREATE INDEX idx_listing_enquiries_customer_user_id ON listing_enquiries (customer_user_id);
