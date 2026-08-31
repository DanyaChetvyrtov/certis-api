--liquibase formatted sql

--changeset dasemenov:260703-2031-create-budgets-table
CREATE TABLE keeper.budgets
(
    id           UUID PRIMARY KEY,
    user_id      UUID           NOT NULL REFERENCES keeper.users (id),

    budget_month   DATE           NOT NULL,
    planned_income NUMERIC(19, 4) NOT NULL,
    savings_target NUMERIC(19, 4) NOT NULL,
    currency       VARCHAR(3)     NOT NULL,

    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,

    CONSTRAINT uq_budgets_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_budgets_user_month
        UNIQUE (user_id, budget_month),
    CONSTRAINT chk_budgets_month_first_day
        CHECK (budget_month = date_trunc('month', budget_month)::date),
    CONSTRAINT chk_budgets_planned_income_non_negative
        CHECK (planned_income >= 0),
    CONSTRAINT chk_budgets_savings_target_non_negative
        CHECK (savings_target >= 0),
    CONSTRAINT chk_budgets_savings_target_within_income
        CHECK (savings_target <= planned_income),
    CONSTRAINT chk_budgets_currency
        CHECK (currency IN ('USD', 'EUR', 'RUB')),
    CONSTRAINT chk_budgets_updated_at
        CHECK (updated_at >= created_at)
);

--rollback DROP TABLE keeper.budgets;
