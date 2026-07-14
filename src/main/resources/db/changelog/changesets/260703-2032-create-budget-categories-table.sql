--liquibase formatted sql

--changeset dasemenov:260703-2032-create-budget-categories-table
CREATE TABLE keeper.budget_categories
(
    id           UUID PRIMARY KEY,
    user_id      UUID           NOT NULL,
    budget_id    UUID           NOT NULL,
    category_id  UUID           NOT NULL,

    limit_amount NUMERIC(19, 4) NOT NULL,

    CONSTRAINT uq_budget_categories_budget_category
        UNIQUE (budget_id, category_id),
    CONSTRAINT fk_budget_categories_budget_user
        FOREIGN KEY (budget_id, user_id)
            REFERENCES keeper.budgets (id, user_id),
    CONSTRAINT fk_budget_categories_category_user
        FOREIGN KEY (category_id, user_id)
            REFERENCES keeper.categories (id, user_id),
    CONSTRAINT chk_budget_categories_limit_non_negative
        CHECK (limit_amount >= 0)
);

--rollback DROP TABLE keeper.budget_categories;
