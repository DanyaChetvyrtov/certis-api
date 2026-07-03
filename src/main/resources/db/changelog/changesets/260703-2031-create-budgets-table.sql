-- liquibase formatted sql

-- changeset dasemenov:260703-2031-create-budgets-table
CREATE TABLE IF NOT EXISTS budgets
(
    id           UUID PRIMARY KEY,
    user_id      UUID REFERENCES users (id),

    month        DATE NOT NULL,
    total_budget NUMERIC(14, 2),

    created_at   TIMESTAMP
)
-- rollback DROP TABLE budgets;
