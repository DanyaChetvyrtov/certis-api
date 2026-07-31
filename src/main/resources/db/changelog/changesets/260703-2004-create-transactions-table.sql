--liquibase formatted sql

--changeset dasemenov:260703-2004-create-transactions-table
CREATE TABLE keeper.transactions
(
    id          UUID PRIMARY KEY,
    user_id     UUID           NOT NULL,
    account_id  UUID           NOT NULL,

    type        VARCHAR(20)    NOT NULL,
    amount      NUMERIC(19, 4) NOT NULL,

    category_id UUID,
    merchant    VARCHAR(255),
    note        TEXT,

    date        TIMESTAMPTZ    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL,

    CONSTRAINT uq_transactions_id_user
        UNIQUE (id, user_id),
    CONSTRAINT fk_transactions_account_user
        FOREIGN KEY (account_id, user_id)
            REFERENCES keeper.accounts (id, user_id),
    CONSTRAINT fk_transactions_category_user
        FOREIGN KEY (category_id, user_id)
            REFERENCES keeper.categories (id, user_id),
    CONSTRAINT chk_transactions_type
        CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_transactions_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_transactions_merchant_not_blank
        CHECK (merchant IS NULL OR btrim(merchant) <> '')
);

--rollback DROP TABLE keeper.transactions;
