CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE users.user_profiles (
    id         BIGINT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    full_name  VARCHAR(255) NOT NULL,
    phone      VARCHAR(50),
    address    VARCHAR(500),
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);
