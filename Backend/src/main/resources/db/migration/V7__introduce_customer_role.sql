ALTER TABLE roles DROP CONSTRAINT chk_roles_name;

INSERT INTO roles (name, description) VALUES
    ('CUSTOMER', 'Registered customer with no platform or organization privileges');

ALTER TABLE roles ADD CONSTRAINT chk_roles_name
    CHECK (name IN ('PLATFORM_ADMIN', 'ORGANIZATION_ADMIN', 'STAFF', 'CUSTOMER'));
