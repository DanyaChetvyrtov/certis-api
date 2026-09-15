--liquibase formatted sql

--changeset dchet:260915-1230-create-budget-optimization-runs
CREATE TABLE keeper.budget_optimization_runs
(
    id                          UUID PRIMARY KEY,
    user_id                     UUID           NOT NULL,
    plan_id                     UUID           NOT NULL,
    forecast_revision_id        UUID           NOT NULL,
    constraint_revision_id      UUID           NOT NULL,

    snapshot_schema_version     SMALLINT       NOT NULL DEFAULT 1,
    algorithm_version           VARCHAR(50)    NOT NULL,
    objective_code              VARCHAR(50)    NOT NULL,
    status                      VARCHAR(20)    NOT NULL,
    generation_idempotency_key  VARCHAR(100)   NOT NULL,
    apply_idempotency_key       VARCHAR(100),
    input_fingerprint           VARCHAR(71)    NOT NULL,

    forecast_income_amount      NUMERIC(19, 4) NOT NULL,
    target_savings_amount       NUMERIC(19, 4) NOT NULL,
    fixed_required_amount       NUMERIC(19, 4) NOT NULL,
    variable_minimum_amount     NUMERIC(19, 4) NOT NULL,
    variable_capacity_amount    NUMERIC(19, 4) NOT NULL,
    maximum_savings_amount      NUMERIC(19, 4) NOT NULL,
    baseline_savings_amount     NUMERIC(19, 4) NOT NULL,

    selected_variable_amount    NUMERIC(19, 4),
    total_allocation_amount     NUMERIC(19, 4),
    actual_savings_amount       NUMERIC(19, 4),
    additional_savings_amount   NUMERIC(19, 4),
    unused_capacity_amount      NUMERIC(19, 4),
    weighted_coverage_score     NUMERIC(19, 8),
    objective_value             NUMERIC(19, 8),

    input_snapshot              JSONB          NOT NULL,
    result_snapshot             JSONB          NOT NULL,
    violations                  JSONB          NOT NULL DEFAULT '[]'::jsonb,

    created_at                  TIMESTAMPTZ    NOT NULL,
    stale_at                    TIMESTAMPTZ,
    dismissed_at                TIMESTAMPTZ,
    applied_at                  TIMESTAMPTZ,

    CONSTRAINT uq_budget_optimization_runs_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_budget_optimization_runs_decision_scope
        UNIQUE (id, user_id, constraint_revision_id),
    CONSTRAINT uq_budget_optimization_runs_generation_key
        UNIQUE (user_id, generation_idempotency_key),
    CONSTRAINT fk_budget_optimization_runs_plan_user
        FOREIGN KEY (plan_id, user_id)
            REFERENCES keeper.budget_plans (id, user_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_budget_optimization_runs_forecast_scope
        FOREIGN KEY (forecast_revision_id, user_id, plan_id)
            REFERENCES keeper.budget_forecast_revisions (id, user_id, plan_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_budget_optimization_runs_constraint_scope
        FOREIGN KEY (constraint_revision_id, user_id, plan_id, forecast_revision_id)
            REFERENCES keeper.budget_constraint_revisions (id, user_id, plan_id, forecast_revision_id)
            ON DELETE CASCADE,
    CONSTRAINT chk_budget_optimization_runs_snapshot_version
        CHECK (snapshot_schema_version > 0),
    CONSTRAINT chk_budget_optimization_runs_algorithm
        CHECK (btrim(algorithm_version) <> ''),
    CONSTRAINT chk_budget_optimization_runs_objective
        CHECK (objective_code = 'MAXIMIZE_WEIGHTED_COVERAGE'),
    CONSTRAINT chk_budget_optimization_runs_status
        CHECK (status IN ('GENERATED', 'INFEASIBLE', 'APPLIED', 'DISMISSED', 'STALE')),
    CONSTRAINT chk_budget_optimization_runs_generation_key
        CHECK (btrim(generation_idempotency_key) <> ''),
    CONSTRAINT chk_budget_optimization_runs_apply_key
        CHECK (apply_idempotency_key IS NULL OR btrim(apply_idempotency_key) <> ''),
    CONSTRAINT chk_budget_optimization_runs_input_hash
        CHECK (input_fingerprint ~ '^sha256:[0-9a-f]{64}$'),
    CONSTRAINT chk_budget_optimization_runs_input_amounts
        CHECK (
            forecast_income_amount >= 0
                AND target_savings_amount >= 0
                AND fixed_required_amount >= 0
                AND variable_minimum_amount >= 0
        ),
    CONSTRAINT chk_budget_optimization_runs_capacity
        CHECK (
            variable_capacity_amount =
                forecast_income_amount - fixed_required_amount - target_savings_amount
                AND maximum_savings_amount =
                forecast_income_amount - fixed_required_amount - variable_minimum_amount
        ),
    CONSTRAINT chk_budget_optimization_runs_snapshots
        CHECK (
            jsonb_typeof(input_snapshot) = 'object'
                AND jsonb_typeof(result_snapshot) = 'object'
                AND jsonb_typeof(violations) = 'array'
        ),
    CONSTRAINT chk_budget_optimization_runs_result_shape
        CHECK (
            (status = 'INFEASIBLE'
                AND maximum_savings_amount < target_savings_amount
                AND selected_variable_amount IS NULL
                AND total_allocation_amount IS NULL
                AND actual_savings_amount IS NULL
                AND additional_savings_amount IS NULL
                AND unused_capacity_amount IS NULL
                AND weighted_coverage_score IS NULL
                AND objective_value IS NULL
                AND jsonb_array_length(violations) > 0)
                OR
            (status <> 'INFEASIBLE'
                AND maximum_savings_amount >= target_savings_amount
                AND selected_variable_amount IS NOT NULL
                AND total_allocation_amount IS NOT NULL
                AND actual_savings_amount IS NOT NULL
                AND additional_savings_amount IS NOT NULL
                AND unused_capacity_amount IS NOT NULL
                AND weighted_coverage_score IS NOT NULL
                AND objective_value IS NOT NULL
                AND selected_variable_amount >= variable_minimum_amount
                AND total_allocation_amount = fixed_required_amount + selected_variable_amount
                AND actual_savings_amount = forecast_income_amount - total_allocation_amount
                AND actual_savings_amount >= target_savings_amount
                AND additional_savings_amount = actual_savings_amount - baseline_savings_amount
                AND unused_capacity_amount = actual_savings_amount - target_savings_amount
                AND weighted_coverage_score >= 0
                AND objective_value >= 0
                AND jsonb_array_length(violations) = 0)
        ),
    CONSTRAINT chk_budget_optimization_runs_lifecycle
        CHECK (
            (status IN ('GENERATED', 'INFEASIBLE')
                AND stale_at IS NULL
                AND dismissed_at IS NULL
                AND applied_at IS NULL
                AND apply_idempotency_key IS NULL)
                OR
            (status = 'STALE'
                AND stale_at IS NOT NULL
                AND dismissed_at IS NULL
                AND applied_at IS NULL
                AND apply_idempotency_key IS NULL)
                OR
            (status = 'DISMISSED'
                AND stale_at IS NULL
                AND dismissed_at IS NOT NULL
                AND applied_at IS NULL
                AND apply_idempotency_key IS NULL)
                OR
            (status = 'APPLIED'
                AND stale_at IS NULL
                AND dismissed_at IS NULL
                AND applied_at IS NOT NULL
                AND apply_idempotency_key IS NOT NULL)
        ),
    CONSTRAINT chk_budget_optimization_runs_event_times
        CHECK (
            (stale_at IS NULL OR stale_at >= created_at)
                AND (dismissed_at IS NULL OR dismissed_at >= created_at)
                AND (applied_at IS NULL OR applied_at >= created_at)
        )
);

CREATE UNIQUE INDEX uq_budget_optimization_runs_generated
    ON keeper.budget_optimization_runs (plan_id)
    WHERE status = 'GENERATED';

CREATE UNIQUE INDEX uq_budget_optimization_runs_apply_key
    ON keeper.budget_optimization_runs (user_id, apply_idempotency_key)
    WHERE apply_idempotency_key IS NOT NULL;

CREATE INDEX ix_budget_optimization_runs_plan_latest
    ON keeper.budget_optimization_runs (plan_id, created_at DESC);

COMMENT ON TABLE keeper.budget_optimization_runs IS
    'Immutable mckp-v1 calculations plus lifecycle metadata; legacy budget_optimizations remains unchanged.';

--rollback DROP TABLE keeper.budget_optimization_runs;

--changeset dchet:260915-1231-create-budget-optimization-decisions
CREATE TABLE keeper.budget_optimization_decisions
(
    id                          UUID PRIMARY KEY,
    user_id                     UUID           NOT NULL,
    optimization_run_id         UUID           NOT NULL,
    constraint_revision_id      UUID           NOT NULL,
    category_constraint_id      UUID           NOT NULL,
    category_id                 UUID           NOT NULL,
    funding_level_id            UUID,

    allocation_type             VARCHAR(20)    NOT NULL,
    constraint_role             VARCHAR(20)    NOT NULL,
    priority                    VARCHAR(20),
    selected_level              VARCHAR(20)    NOT NULL,
    current_limit_amount        NUMERIC(19, 4) NOT NULL,
    minimum_amount              NUMERIC(19, 4) NOT NULL,
    recommended_limit_amount    NUMERIC(19, 4) NOT NULL,
    change_amount               NUMERIC(19, 4) NOT NULL,
    coverage                    NUMERIC(7, 6)  NOT NULL,
    option_value                NUMERIC(19, 8) NOT NULL,
    reason_code                 VARCHAR(50)    NOT NULL,
    reason_parameters           JSONB          NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT uq_budget_optimization_decisions_run_category
        UNIQUE (optimization_run_id, category_id),
    CONSTRAINT fk_budget_optimization_decisions_run_scope
        FOREIGN KEY (optimization_run_id, user_id, constraint_revision_id)
            REFERENCES keeper.budget_optimization_runs (id, user_id, constraint_revision_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_budget_optimization_decisions_category_scope
        FOREIGN KEY (
            category_constraint_id,
            user_id,
            constraint_revision_id,
            category_id
        )
            REFERENCES keeper.budget_category_constraints (
                id,
                user_id,
                constraint_revision_id,
                category_id
            ),
    CONSTRAINT fk_budget_optimization_decisions_funding_scope
        FOREIGN KEY (funding_level_id, user_id, category_constraint_id)
            REFERENCES keeper.budget_constraint_funding_levels (id, user_id, category_constraint_id),

    CONSTRAINT chk_budget_optimization_decisions_allocation_type
        CHECK (allocation_type IN ('FIXED', 'VARIABLE')),
    CONSTRAINT chk_budget_optimization_decisions_role
        CHECK (constraint_role IN ('REQUIRED', 'FLEXIBLE')),
    CONSTRAINT chk_budget_optimization_decisions_priority
        CHECK (priority IS NULL OR priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_budget_optimization_decisions_level
        CHECK (selected_level IN ('FIXED', 'MINIMUM', 'BALANCED', 'COMFORTABLE')),
    CONSTRAINT chk_budget_optimization_decisions_amounts
        CHECK (
            current_limit_amount >= 0
                AND minimum_amount >= 0
                AND recommended_limit_amount >= minimum_amount
                AND change_amount = recommended_limit_amount - current_limit_amount
        ),
    CONSTRAINT chk_budget_optimization_decisions_score
        CHECK (coverage >= 0 AND coverage <= 1 AND option_value >= 0),
    CONSTRAINT chk_budget_optimization_decisions_mckp_shape
        CHECK (
            (allocation_type = 'FIXED'
                AND constraint_role = 'REQUIRED'
                AND priority IS NULL
                AND selected_level = 'FIXED'
                AND funding_level_id IS NULL
                AND recommended_limit_amount = minimum_amount
                AND coverage = 1
                AND option_value = 0)
                OR
            (allocation_type = 'VARIABLE'
                AND priority IS NOT NULL
                AND selected_level IN ('MINIMUM', 'BALANCED', 'COMFORTABLE')
                AND funding_level_id IS NOT NULL)
        ),
    CONSTRAINT chk_budget_optimization_decisions_reason
        CHECK (btrim(reason_code) <> ''),
    CONSTRAINT chk_budget_optimization_decisions_reason_object
        CHECK (jsonb_typeof(reason_parameters) = 'object')
);

CREATE INDEX ix_budget_optimization_decisions_run
    ON keeper.budget_optimization_decisions (optimization_run_id);

--rollback DROP TABLE keeper.budget_optimization_decisions;
