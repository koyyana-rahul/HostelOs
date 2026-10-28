CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_name CHECK (name IN ('ADMIN', 'STAFF'))
);

CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    login_identifier VARCHAR(254) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_user_accounts PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_login_identifier UNIQUE (login_identifier),
    CONSTRAINT chk_user_accounts_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_user_accounts_login_identifier CHECK (login_identifier = LOWER(TRIM(login_identifier))),
    CONSTRAINT chk_user_accounts_login_length CHECK (CHAR_LENGTH(login_identifier) BETWEEN 3 AND 254),
    CONSTRAINT chk_user_accounts_password_hash CHECK (CHAR_LENGTH(password_hash) >= 60)
);

CREATE INDEX idx_user_accounts_status ON user_accounts (status);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Administrative identity and privileged configuration'),
    ('STAFF', 'Authenticated operational staff');
