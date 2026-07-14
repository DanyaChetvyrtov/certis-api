--liquibase formatted sql

--changeset dasemenov:260703-2059-create-budget-optimizations-table
CREATE TABLE keeper.budget_optimizations
(
    id              UUID PRIMARY KEY,
    user_id         UUID           NOT NULL,
    budget_id       UUID           NOT NULL,

    input_snapshot  JSONB          NOT NULL, -- income, expenses, constraints
    result_snapshot JSONB          NOT NULL, -- optimized allocation

    savings_before  NUMERIC(19, 4) NOT NULL,
    savings_after   NUMERIC(19, 4) NOT NULL,

    created_at      TIMESTAMPTZ    NOT NULL,

    CONSTRAINT fk_budget_optimizations_budget_user
        FOREIGN KEY (budget_id, user_id)
            REFERENCES keeper.budgets (id, user_id),
    CONSTRAINT chk_budget_optimizations_input_object
        CHECK (jsonb_typeof(input_snapshot) = 'object'),
    CONSTRAINT chk_budget_optimizations_result_object
        CHECK (jsonb_typeof(result_snapshot) = 'object')
);

--rollback DROP TABLE keeper.budget_optimizations;
