-- liquibase formatted sql

-- changeset dasemenov:260703-2034-create-transactions-table
CREATE TABLE IF NOT EXISTS keeper.recurring_transactions
(
    id            UUID PRIMARY KEY,
    user_id       UUID REFERENCES keeper.users (id),
    account_id    UUID REFERENCES keeper.accounts (id),

    type          VARCHAR(50)    NOT NULL,
    amount        NUMERIC(14, 2) NOT NULL,

    category_id   UUID,
    frequency     VARCHAR(100)   NOT NULL,

    next_run_date DATE           NOT NULL,
    is_active     BOOLEAN        NOT NULL
)
-- rollback DROP TABLE recurring_transactions;
