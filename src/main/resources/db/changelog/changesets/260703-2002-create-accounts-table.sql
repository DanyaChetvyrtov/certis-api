-- liquibase formatted sql

-- changeset dasemenov:260703-2002-create-accounts-table
CREATE TABLE IF NOT EXISTS keeper.accounts
(
    id         UUID PRIMARY KEY,
    user_id    UUID REFERENCES keeper.users (id),
    name       TEXT NOT NULL,
    type       TEXT,
    balance    NUMERIC(14, 2),
    currency   TEXT,
    created_at TIMESTAMP
)
-- rollback DROP TABLE accounts;
