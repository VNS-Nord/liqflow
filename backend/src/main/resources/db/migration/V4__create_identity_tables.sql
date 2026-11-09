CREATE TABLE users
(
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    security_version BIGINT NOT NULL DEFAULT 0,
    password_changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE roles
(
    id UUID PRIMARY KEY,
    code VARCHAR(50)  NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT uk_role_code UNIQUE (code)
);

CREATE TABLE permissions
(
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT uk_permission_code UNIQUE (code)
);

CREATE TABLE user_roles
(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE role_permissions
(
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE TABLE user_locations
(
    user_id UUID NOT NULL,
    location_id UUID NOT NULL,
    CONSTRAINT pk_user_locations PRIMARY KEY (user_id, location_id),
    CONSTRAINT fk_user_locations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_locations_location FOREIGN KEY (location_id) REFERENCES locations (id)
);

CREATE INDEX idx_user_roles_role ON user_roles (role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions (permission_id);
CREATE INDEX idx_user_locations_location ON user_locations (location_id);