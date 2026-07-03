-- liquibase formatted sql

-- changeset dasemenov:260703-2031-create-budget-categories-table
CREATE TABLE IF NOT EXISTS budget_categories
(
    id           UUID PRIMARY KEY,
    budget_id    UUID REFERENCES budgets (id),
    category_id  UUID REFERENCES categories (id),

    limit_amount NUMERIC(14, 2) NOT NULL,
    spent_amount NUMERIC(14, 2)
)
-- rollback DROP TABLE budget_categories;
