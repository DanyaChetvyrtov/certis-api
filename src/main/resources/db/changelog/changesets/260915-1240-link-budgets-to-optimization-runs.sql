--liquibase formatted sql

--changeset dchet:260915-1240-link-budgets-to-optimization-runs
ALTER TABLE keeper.budgets
    ADD COLUMN source_optimization_id UUID,
    ADD CONSTRAINT uq_budgets_source_optimization
        UNIQUE (source_optimization_id),
    ADD CONSTRAINT fk_budgets_source_optimization_user
        FOREIGN KEY (source_optimization_id, user_id)
            REFERENCES keeper.budget_optimization_runs (id, user_id);

COMMENT ON COLUMN keeper.budgets.source_optimization_id IS
    'Planning optimization run that last materialized the operational monthly budget.';

--rollback ALTER TABLE keeper.budgets DROP CONSTRAINT fk_budgets_source_optimization_user, DROP CONSTRAINT uq_budgets_source_optimization, DROP COLUMN source_optimization_id;
