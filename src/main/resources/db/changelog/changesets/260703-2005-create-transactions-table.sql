--liquibase formatted sql

--changeset dasemenov:260703-2005-create-transactions-table
CREATE TABLE keeper.transactions
(
    id                                UUID PRIMARY KEY,

    user_id                           UUID           NOT NULL,
    account_id                        UUID           NOT NULL,
    category_id                       UUID,
    recurring_transaction_template_id UUID,

    type                              VARCHAR(20)    NOT NULL,
    amount                            NUMERIC(19, 4) NOT NULL,
    merchant                          VARCHAR(255),
    note                              TEXT,

    scheduled_for                     DATE,
    occurred_at                       TIMESTAMPTZ    NOT NULL,
    created_at                        TIMESTAMPTZ    NOT NULL,
    updated_at                        TIMESTAMPTZ    NOT NULL,
    deleted_at                        TIMESTAMPTZ,

    CONSTRAINT fk_transactions_account_user
        FOREIGN KEY (account_id, user_id)
            REFERENCES keeper.accounts (id, user_id),
    CONSTRAINT fk_transactions_category_user_type
        FOREIGN KEY (category_id, user_id, type)
            REFERENCES keeper.categories (id, user_id, type),
    CONSTRAINT fk_transactions_recurring_transaction_template_user
        FOREIGN KEY (recurring_transaction_template_id, user_id)
            REFERENCES keeper.recurring_transaction_templates (id, user_id),

    CONSTRAINT uq_transactions_id_user
        UNIQUE (id, user_id),

    CONSTRAINT chk_transactions_type
        CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_transactions_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_transactions_merchant_not_blank
        CHECK (merchant IS NULL OR btrim(merchant) <> ''),
    CONSTRAINT chk_transactions_updated_at
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_transactions_deleted_at
        CHECK (deleted_at IS NULL OR deleted_at >= updated_at),
    CONSTRAINT chk_transactions_recurring_origin
        CHECK (
            (recurring_transaction_template_id IS NULL AND scheduled_for IS NULL)
                OR
            (recurring_transaction_template_id IS NOT NULL AND scheduled_for IS NOT NULL)
            )
);

CREATE INDEX ix_transactions_user_occurred_at_active
    ON keeper.transactions (user_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_transactions_user_account_type_occurred_at_active
    ON keeper.transactions (user_id, account_id, type, occurred_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_transactions_user_category_occurred_at_active
    ON keeper.transactions (user_id, category_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_transactions_recurring_occurrence
    ON keeper.transactions (recurring_transaction_template_id, scheduled_for)
    WHERE recurring_transaction_template_id IS NOT NULL;

--rollback DROP TABLE keeper.transactions;
