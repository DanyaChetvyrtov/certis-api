-- liquibase formatted sql

-- changeset dasemenov:260703-1954-create-users-table
CREATE TABLE IF NOT EXISTS users
(
    id                 UUID PRIMARY KEY,
    email              VARCHAR(255) UNIQUE NOT NULL,
    password_hash      VARCHAR             NOT NULL,
    preferred_currency CHAR(3)             NOT NULL,
    created_at         TIMESTAMP
)
-- rollback DROP TABLE users;
