--liquibase formatted sql

--changeset dasemenov:260703-2059-create-budget-optimizations-table
CREATE TABLE keeper.budget_optimizations
(
    id              UUID PRIMARY KEY,
    user_id         UUID           NOT NULL,
    budget_id       UUID           NOT NULL,

    snapshot_schema_version SMALLINT       NOT NULL DEFAULT 1,
    algorithm_version       VARCHAR(50)    NOT NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'PROPOSED',

    input_snapshot          JSONB          NOT NULL, -- income, expenses, constraints
    result_snapshot         JSONB          NOT NULL, -- optimized allocation

    savings_before          NUMERIC(19, 4) NOT NULL,
    savings_after           NUMERIC(19, 4) NOT NULL,

    created_at              TIMESTAMPTZ    NOT NULL,
    applied_at              TIMESTAMPTZ,

    CONSTRAINT fk_budget_optimizations_budget_user
        FOREIGN KEY (budget_id, user_id)
            REFERENCES keeper.budgets (id, user_id)
            ON DELETE CASCADE,
    CONSTRAINT chk_budget_optimizations_snapshot_schema_version
        CHECK (snapshot_schema_version > 0),
    CONSTRAINT chk_budget_optimizations_algorithm_version_not_blank
        CHECK (btrim(algorithm_version) <> ''),
    CONSTRAINT chk_budget_optimizations_status
        CHECK (status IN ('PROPOSED', 'APPLIED', 'DISMISSED')),
    CONSTRAINT chk_budget_optimizations_input_object
        CHECK (jsonb_typeof(input_snapshot) = 'object'),
    CONSTRAINT chk_budget_optimizations_result_object
        CHECK (jsonb_typeof(result_snapshot) = 'object'),
    CONSTRAINT chk_budget_optimizations_application_state
        CHECK (
            (status = 'APPLIED' AND applied_at IS NOT NULL)
                OR
            (status <> 'APPLIED' AND applied_at IS NULL)
            ),
    CONSTRAINT chk_budget_optimizations_applied_at
        CHECK (applied_at IS NULL OR applied_at >= created_at)
);

CREATE INDEX ix_budget_optimizations_budget_created_at
    ON keeper.budget_optimizations (budget_id, created_at DESC);

--rollback DROP TABLE keeper.budget_optimizations;
