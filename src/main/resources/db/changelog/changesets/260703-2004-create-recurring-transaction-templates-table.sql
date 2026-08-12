--liquibase formatted sql

--changeset dasemenov:260703-2004-create-recurring-transaction-templates-table
CREATE TABLE keeper.recurring_transaction_templates
(
    id             UUID PRIMARY KEY,

    user_id        UUID           NOT NULL,
    account_id     UUID           NOT NULL,
    category_id    UUID,

    name           VARCHAR(150)   NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    amount         NUMERIC(19, 4) NOT NULL,
    merchant       VARCHAR(255),
    note           TEXT,

    status         VARCHAR(20)    NOT NULL,
    frequency      VARCHAR(20)    NOT NULL,
    interval_count SMALLINT       NOT NULL,
    start_date     DATE           NOT NULL,
    end_date       DATE,
    last_run_date  DATE,
    next_run_date  DATE,

    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,

    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    retry_after TIMESTAMPTZ,

    CONSTRAINT fk_recurring_transaction_templates_account_user
        FOREIGN KEY (account_id, user_id)
            REFERENCES keeper.accounts (id, user_id),
    CONSTRAINT fk_recurring_transaction_templates_category_user_type
        FOREIGN KEY (category_id, user_id, type)
            REFERENCES keeper.categories (id, user_id, type),

    CONSTRAINT uq_recurring_transaction_templates_id_user
        UNIQUE (id, user_id),

    CONSTRAINT chk_recurring_transaction_templates_type
        CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_recurring_transaction_templates_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_recurring_transaction_templates_name_not_blank
        CHECK (btrim(name) <> ''),
    CONSTRAINT chk_recurring_transaction_templates_merchant_not_blank
        CHECK (merchant IS NULL OR btrim(merchant) <> ''),
    CONSTRAINT chk_recurring_transaction_templates_frequency
        CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    CONSTRAINT chk_recurring_transaction_templates_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_recurring_transaction_templates_interval_positive
        CHECK (interval_count > 0),
    CONSTRAINT chk_recurring_transaction_templates_date_range
        CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_recurring_transaction_templates_last_run_range
        CHECK (
            last_run_date IS NULL
                OR (
                    last_run_date >= start_date
                    AND (end_date IS NULL OR last_run_date <= end_date)
                )
        ),
    CONSTRAINT chk_recurring_transaction_templates_next_run_range
        CHECK (
            next_run_date IS NULL
                OR (
                    next_run_date >= start_date
                    AND (end_date IS NULL OR next_run_date <= end_date)
                )
        ),
    CONSTRAINT chk_recurring_transaction_templates_run_order
        CHECK (
            last_run_date IS NULL
                OR next_run_date IS NULL
                OR next_run_date > last_run_date
        ),
    CONSTRAINT chk_recurring_transaction_templates_status_next_run
        CHECK (
            (status IN ('ACTIVE', 'PAUSED') AND next_run_date IS NOT NULL)
                OR
            (status IN ('COMPLETED', 'CANCELLED') AND next_run_date IS NULL)
        ),
    CONSTRAINT chk_recurring_transaction_templates_updated_at
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_recurring_transaction_templates_consecutive_failures
        CHECK (consecutive_failures >= 0)
);

CREATE INDEX ix_recurring_transaction_templates_due
    ON keeper.recurring_transaction_templates (next_run_date, id)
    WHERE status = 'ACTIVE';

CREATE INDEX ix_recurring_transaction_templates_user_category_schedulable
    ON keeper.recurring_transaction_templates (user_id, category_id)
    WHERE category_id IS NOT NULL
        AND status IN ('ACTIVE', 'PAUSED');

CREATE INDEX ix_recurring_transaction_templates_user_account_schedulable
    ON keeper.recurring_transaction_templates (user_id, account_id)
    WHERE status IN ('ACTIVE', 'PAUSED');

--rollback DROP TABLE keeper.recurring_transaction_templates;
