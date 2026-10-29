CREATE TABLE organizations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_organizations PRIMARY KEY (id),
    CONSTRAINT uk_organizations_slug UNIQUE (slug),
    CONSTRAINT chk_organizations_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED')),
    CONSTRAINT chk_organizations_slug CHECK (slug = LOWER(TRIM(slug))),
    CONSTRAINT chk_organizations_slug_length CHECK (CHAR_LENGTH(slug) BETWEEN 3 AND 60),
    CONSTRAINT chk_organizations_name_length CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 120)
);

CREATE TABLE organization_memberships (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    org_role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_organization_memberships PRIMARY KEY (id),
    CONSTRAINT fk_org_memberships_org FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_org_memberships_user FOREIGN KEY (user_id) REFERENCES user_accounts (id) ON DELETE RESTRICT,
    CONSTRAINT uk_org_memberships_org_user UNIQUE (organization_id, user_id),
    CONSTRAINT uk_org_memberships_tenant_id UNIQUE (organization_id, id),
    CONSTRAINT chk_org_memberships_role CHECK (org_role IN ('ORG_ADMIN', 'ORG_STAFF')),
    CONSTRAINT chk_org_memberships_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED'))
);

CREATE INDEX idx_org_memberships_user_id ON organization_memberships (user_id);
