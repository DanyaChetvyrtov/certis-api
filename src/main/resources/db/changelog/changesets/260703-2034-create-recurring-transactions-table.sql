--liquibase formatted sql

--changeset dasemenov:260703-2034-create-recurring-transactions-table
CREATE TABLE keeper.recurring_transactions
(
    id            UUID PRIMARY KEY,
    user_id       UUID           NOT NULL,
    account_id    UUID           NOT NULL,

    type          VARCHAR(20)    NOT NULL,
    amount        NUMERIC(19, 4) NOT NULL,

    category_id   UUID,
    frequency     VARCHAR(20)    NOT NULL,

    next_run_date DATE           NOT NULL,
    is_active     BOOLEAN        NOT NULL,

    CONSTRAINT uq_recurring_transactions_id_user
        UNIQUE (id, user_id),
    CONSTRAINT fk_recurring_transactions_account_user
        FOREIGN KEY (account_id, user_id)
            REFERENCES keeper.accounts (id, user_id),
    CONSTRAINT fk_recurring_transactions_category_user
        FOREIGN KEY (category_id, user_id)
            REFERENCES keeper.categories (id, user_id),
    CONSTRAINT chk_recurring_transactions_type
        CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_recurring_transactions_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_recurring_transactions_frequency
        CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'))
);

--rollback DROP TABLE keeper.recurring_transactions;
