--liquibase formatted sql

--changeset dchet:260915-1190-scope-budgets-by-currency
ALTER TABLE keeper.budgets
    DROP CONSTRAINT uq_budgets_user_month,
    ADD CONSTRAINT uq_budgets_user_month_currency
        UNIQUE (user_id, budget_month, currency),
    ADD CONSTRAINT uq_budgets_id_user_scope
        UNIQUE (id, user_id, budget_month, currency);

--rollback ALTER TABLE keeper.budgets DROP CONSTRAINT uq_budgets_id_user_scope, DROP CONSTRAINT uq_budgets_user_month_currency, ADD CONSTRAINT uq_budgets_user_month UNIQUE (user_id, budget_month);
