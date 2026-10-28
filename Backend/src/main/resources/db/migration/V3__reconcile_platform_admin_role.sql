ALTER TABLE roles DROP CONSTRAINT chk_roles_name;

UPDATE roles
SET name = 'PLATFORM_ADMIN',
    description = 'Platform-wide administrator for platform identity and privileged configuration'
WHERE name = 'ADMIN';

INSERT INTO roles (name, description) VALUES
    ('ORGANIZATION_ADMIN', 'Organization administrator; tenant-scoped enforcement is deferred until organization membership exists');

UPDATE roles
SET description = 'Authenticated operational staff for future hostel workflows'
WHERE name = 'STAFF';

ALTER TABLE roles ADD CONSTRAINT chk_roles_name
    CHECK (name IN ('PLATFORM_ADMIN', 'ORGANIZATION_ADMIN', 'STAFF'));
