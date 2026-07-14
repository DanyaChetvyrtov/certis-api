--liquibase formatted sql

--changeset dasemenov:260703-2031-create-budgets-table
CREATE TABLE keeper.budgets
(
    id           UUID PRIMARY KEY,
    user_id      UUID           NOT NULL REFERENCES keeper.users (id),

    period_start DATE           NOT NULL,
    period_end   DATE           NOT NULL,
    total_budget NUMERIC(19, 4) NOT NULL,
    currency     VARCHAR(3)     NOT NULL,

    created_at   TIMESTAMPTZ    NOT NULL,

    CONSTRAINT uq_budgets_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_budgets_user_period
        UNIQUE (user_id, period_start, period_end),
    CONSTRAINT chk_budgets_period
        CHECK (period_start < period_end),
    CONSTRAINT chk_budgets_total_budget_non_negative
        CHECK (total_budget >= 0),
    CONSTRAINT chk_budgets_currency
        CHECK (currency IN ('USD', 'EUR', 'RUB'))
);

--rollback DROP TABLE keeper.budgets;
