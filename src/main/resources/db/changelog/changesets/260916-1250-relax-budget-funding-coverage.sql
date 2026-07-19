--liquibase formatted sql

--changeset dchet:260916-1250-relax-budget-funding-coverage
-- Coverage is calculated from the chosen amount and confirmed category forecast,
-- rather than inferred from the MINIMUM / BALANCED / COMFORTABLE level name.
ALTER TABLE keeper.budget_constraint_funding_levels
    DROP CONSTRAINT chk_budget_funding_levels_coverage;

ALTER TABLE keeper.budget_constraint_funding_levels
    ADD CONSTRAINT chk_budget_funding_levels_coverage
        CHECK (coverage BETWEEN 0.000000 AND 1.000000);

--rollback ALTER TABLE keeper.budget_constraint_funding_levels DROP CONSTRAINT chk_budget_funding_levels_coverage;
--rollback ALTER TABLE keeper.budget_constraint_funding_levels ADD CONSTRAINT chk_budget_funding_levels_coverage CHECK ((level = 'MINIMUM' AND coverage = 0.600000) OR (level = 'BALANCED' AND coverage = 0.850000) OR (level = 'COMFORTABLE' AND coverage = 1.000000));
