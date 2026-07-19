--liquibase formatted sql

--changeset dasemenov:260703-2032-create-budget-categories-table
CREATE TABLE keeper.budget_categories
(
    id            UUID PRIMARY KEY,
    user_id       UUID           NOT NULL,
    budget_id     UUID           NOT NULL,
    category_id   UUID           NOT NULL,
    category_type VARCHAR(20)    NOT NULL DEFAULT 'EXPENSE',

    limit_amount  NUMERIC(19, 4) NOT NULL,
    expense_type  VARCHAR(20)    NOT NULL,

    CONSTRAINT uq_budget_categories_budget_category
        UNIQUE (budget_id, category_id),
    CONSTRAINT fk_budget_categories_budget_user
        FOREIGN KEY (budget_id, user_id)
            REFERENCES keeper.budgets (id, user_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_budget_categories_expense_category_user
        FOREIGN KEY (category_id, user_id, category_type)
            REFERENCES keeper.categories (id, user_id, type),
    CONSTRAINT chk_budget_categories_category_type
        CHECK (category_type = 'EXPENSE'),
    CONSTRAINT chk_budget_categories_limit_non_negative
        CHECK (limit_amount >= 0),
    CONSTRAINT chk_budget_categories_expense_type
        CHECK (expense_type IN ('FIXED', 'VARIABLE'))
);

CREATE INDEX ix_budget_categories_user_category
    ON keeper.budget_categories (user_id, category_id);

--rollback DROP TABLE keeper.budget_categories;

--changeset dasemenov:260703-2032-create-budget-allocation-validation-function splitStatements:false

CREATE FUNCTION keeper.ensure_budget_allocation_is_valid()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    category_archived_at TIMESTAMPTZ;
    planned_income       NUMERIC(19, 4);
    savings_target       NUMERIC(19, 4);
    allocated_amount     NUMERIC(19, 4);
BEGIN
    SELECT archived_at
    INTO category_archived_at
    FROM keeper.categories
    WHERE id = NEW.category_id
      AND user_id = NEW.user_id
      AND type = 'EXPENSE'
        FOR SHARE;

    IF FOUND AND category_archived_at IS NOT NULL THEN
        RAISE EXCEPTION 'Archived category % cannot be assigned to a budget', NEW.category_id
            USING ERRCODE = '23514',
                CONSTRAINT = 'chk_budget_categories_category_active';
    END IF;

    SELECT b.planned_income, b.savings_target
    INTO planned_income, savings_target
    FROM keeper.budgets b
    WHERE b.id = NEW.budget_id
      AND b.user_id = NEW.user_id
        FOR UPDATE;

    IF NOT FOUND THEN
        RETURN NEW;
    END IF;

    SELECT COALESCE(SUM(bc.limit_amount), 0)
    INTO allocated_amount
    FROM keeper.budget_categories bc
    WHERE bc.budget_id = NEW.budget_id
      AND bc.id <> NEW.id;

    IF allocated_amount + NEW.limit_amount + savings_target > planned_income THEN
        RAISE EXCEPTION 'Budget allocations and savings target exceed planned income'
            USING ERRCODE = '23514',
                CONSTRAINT = 'chk_budget_allocations_within_income';
    END IF;

    RETURN NEW;
END;
$$;

--rollback DROP FUNCTION keeper.ensure_budget_allocation_is_valid();

--changeset dasemenov:260703-2032-create-budget-capacity-validation-function splitStatements:false

CREATE FUNCTION keeper.ensure_budget_capacity_is_valid()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    allocated_amount NUMERIC(19, 4);
BEGIN
    SELECT COALESCE(SUM(bc.limit_amount), 0)
    INTO allocated_amount
    FROM keeper.budget_categories bc
    WHERE bc.budget_id = NEW.id;

    IF allocated_amount + NEW.savings_target > NEW.planned_income THEN
        RAISE EXCEPTION 'Budget allocations and savings target exceed planned income'
            USING ERRCODE = '23514',
                CONSTRAINT = 'chk_budget_allocations_within_income';
    END IF;

    RETURN NEW;
END;
$$;

--rollback DROP FUNCTION keeper.ensure_budget_capacity_is_valid();

--changeset dasemenov:260703-2032-create-budget-validation-triggers

CREATE TRIGGER trg_budget_categories_validate_allocation
    BEFORE INSERT OR UPDATE OF budget_id, user_id, category_id, limit_amount
    ON keeper.budget_categories
    FOR EACH ROW
EXECUTE FUNCTION keeper.ensure_budget_allocation_is_valid();

CREATE TRIGGER trg_budgets_validate_capacity
    BEFORE UPDATE OF planned_income, savings_target
    ON keeper.budgets
    FOR EACH ROW
    WHEN (
        OLD.planned_income IS DISTINCT FROM NEW.planned_income
            OR OLD.savings_target IS DISTINCT FROM NEW.savings_target
        )
EXECUTE FUNCTION keeper.ensure_budget_capacity_is_valid();

--rollback DROP TRIGGER trg_budgets_validate_capacity ON keeper.budgets; DROP TRIGGER trg_budget_categories_validate_allocation ON keeper.budget_categories;
