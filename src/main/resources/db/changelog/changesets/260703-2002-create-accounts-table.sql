--liquibase formatted sql

--changeset dasemenov:260703-2002-create-accounts-table
CREATE TABLE keeper.accounts
(
    id              UUID PRIMARY KEY,
    user_id         UUID           NOT NULL REFERENCES keeper.users (id),
    name            VARCHAR(100)   NOT NULL,
    type            VARCHAR(20)    NOT NULL,
    opening_balance NUMERIC(19, 4) NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL,
    closed_at       TIMESTAMPTZ,

    CONSTRAINT uq_accounts_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_accounts_id_user_currency
        UNIQUE (id, user_id, currency),
    CONSTRAINT chk_accounts_name_not_blank
        CHECK (btrim(name) <> ''),
    CONSTRAINT chk_accounts_type
        CHECK (type IN ('CASH', 'BANK', 'CARD', 'INVESTMENT')),
    CONSTRAINT chk_accounts_currency
        CHECK (currency IN ('USD', 'EUR', 'RUB')),
    CONSTRAINT chk_accounts_closed_at
        CHECK (closed_at IS NULL OR closed_at >= created_at)
);

--rollback DROP TABLE keeper.accounts;
