ALTER TABLE organization_memberships DROP CONSTRAINT chk_org_memberships_role;

UPDATE organization_memberships
SET org_role = 'ORGANIZATION_ADMIN'
WHERE org_role = 'ORG_ADMIN';

ALTER TABLE organization_memberships ADD CONSTRAINT chk_org_memberships_role
    CHECK (org_role IN ('ORGANIZATION_ADMIN', 'ORG_STAFF'));

ALTER TABLE organizations ADD COLUMN owner_user_id BIGINT;

UPDATE organizations o
SET owner_user_id = (
    SELECT MIN(m.user_id)
    FROM organization_memberships m
    WHERE m.organization_id = o.id
      AND m.status = 'ACTIVE'
      AND m.org_role = 'ORGANIZATION_ADMIN'
)
WHERE o.owner_user_id IS NULL;

ALTER TABLE organizations ADD CONSTRAINT fk_organizations_owner
    FOREIGN KEY (owner_user_id) REFERENCES user_accounts (id) ON DELETE RESTRICT;

CREATE INDEX idx_organizations_owner_user_id ON organizations (owner_user_id);
