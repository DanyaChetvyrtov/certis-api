-- liquibase formatted sql

-- changeset dasemenov:260703-2004-create-transactions-table
CREATE TABLE IF NOT EXISTS transactions
(
    id          UUID PRIMARY KEY,
    user_id     UUID REFERENCES users (id),
    account_id  UUID REFERENCES accounts (id),

    type        VARCHAR(50)    NOT NULL,
    amount      NUMERIC(14, 2) NOT NULL,

    category_id UUID,
    merchant    VARCHAR(255),
    note        TEXT,

    date        TIMESTAMP      NOT NULL,
    created_at  TIMESTAMP
)
-- rollback DROP TABLE transactions;
