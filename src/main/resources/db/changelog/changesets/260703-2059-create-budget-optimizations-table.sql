-- liquibase formatted sql

-- changeset dasemenov:260703-2059-create-budget-optimizations-table
CREATE TABLE IF NOT EXISTS budget_optimizations
(
    id              UUID PRIMARY KEY,
    user_id         UUID REFERENCES users (id),

    month           DATE,
    input_snapshot  JSONB, -- income, expenses, constraints
    result_snapshot JSONB, -- optimized allocation

    savings_before  NUMERIC(14, 2),
    savings_after   NUMERIC(14, 2),

    created_at      TIMESTAMP
)
-- rollback DROP TABLE budget_optimizations;
