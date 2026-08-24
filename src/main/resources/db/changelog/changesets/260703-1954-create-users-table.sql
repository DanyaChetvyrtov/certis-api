--liquibase formatted sql

--changeset dasemenov:260703-1954-create-users-table
CREATE TABLE keeper.users
(
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT         NOT NULL,
    last_login    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_users_email_not_blank
        CHECK (btrim(email) <> ''),
    CONSTRAINT chk_users_email_normalized
        CHECK (email = lower(btrim(email))),
    CONSTRAINT chk_users_password_hash_not_blank
        CHECK (btrim(password_hash) <> '')
);

--rollback DROP TABLE keeper.users;
