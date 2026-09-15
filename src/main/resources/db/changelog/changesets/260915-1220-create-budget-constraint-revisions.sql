--liquibase formatted sql

--changeset dchet:260915-1220-create-budget-constraint-revisions
CREATE TABLE keeper.budget_constraint_revisions
(
    id                       UUID PRIMARY KEY,
    user_id                  UUID           NOT NULL,
    plan_id                  UUID           NOT NULL,
    forecast_revision_id     UUID           NOT NULL,
    revision                 INTEGER        NOT NULL,

    constraint_fingerprint   VARCHAR(71)    NOT NULL,
    target_savings_amount    NUMERIC(19, 4) NOT NULL,
    fixed_required_amount    NUMERIC(19, 4) NOT NULL,
    variable_minimum_amount  NUMERIC(19, 4) NOT NULL,
    maximum_savings_amount   NUMERIC(19, 4) NOT NULL,
    feasibility_status       VARCHAR(20)    NOT NULL,
    shortfall_amount         NUMERIC(19, 4) NOT NULL,
    violations               JSONB          NOT NULL DEFAULT '[]'::jsonb,

    created_at               TIMESTAMPTZ    NOT NULL,

    CONSTRAINT uq_budget_constraints_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_budget_constraints_id_user_plan
        UNIQUE (id, user_id, plan_id),
    CONSTRAINT uq_budget_constraints_run_scope
        UNIQUE (id, user_id, plan_id, forecast_revision_id),
    CONSTRAINT uq_budget_constraints_plan_revision
        UNIQUE (plan_id, revision),
    CONSTRAINT fk_budget_constraints_plan_user
        FOREIGN KEY (plan_id, user_id)
            REFERENCES keeper.budget_plans (id, user_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_budget_constraints_forecast_scope
        FOREIGN KEY (forecast_revision_id, user_id, plan_id)
            REFERENCES keeper.budget_forecast_revisions (id, user_id, plan_id)
            ON DELETE CASCADE,

    CONSTRAINT chk_budget_constraints_revision_positive
        CHECK (revision > 0),
    CONSTRAINT chk_budget_constraints_fingerprint
        CHECK (constraint_fingerprint ~ '^sha256:[0-9a-f]{64}$'),
    CONSTRAINT chk_budget_constraints_non_negative
        CHECK (
            target_savings_amount >= 0
                AND fixed_required_amount >= 0
                AND variable_minimum_amount >= 0
                AND shortfall_amount >= 0
        ),
    CONSTRAINT chk_budget_constraints_feasibility
        CHECK (feasibility_status IN ('FEASIBLE', 'INFEASIBLE')),
    CONSTRAINT chk_budget_constraints_violations_array
        CHECK (jsonb_typeof(violations) = 'array'),
    CONSTRAINT chk_budget_constraints_feasibility_result
        CHECK (
            (feasibility_status = 'FEASIBLE'
                AND maximum_savings_amount >= target_savings_amount
                AND shortfall_amount = 0
                AND jsonb_array_length(violations) = 0)
                OR
            (feasibility_status = 'INFEASIBLE'
                AND maximum_savings_amount < target_savings_amount
                AND shortfall_amount = target_savings_amount - maximum_savings_amount
                AND jsonb_array_length(violations) > 0)
        )
);

CREATE INDEX ix_budget_constraints_plan_latest
    ON keeper.budget_constraint_revisions (plan_id, revision DESC);

COMMENT ON TABLE keeper.budget_constraint_revisions IS
    'Immutable savings target and category-constraint snapshots for one confirmed forecast revision.';

--rollback DROP TABLE keeper.budget_constraint_revisions;

--changeset dchet:260915-1221-create-budget-category-constraints
CREATE TABLE keeper.budget_category_constraints
(
    id                       UUID PRIMARY KEY,
    user_id                  UUID           NOT NULL,
    constraint_revision_id   UUID           NOT NULL,
    category_id              UUID           NOT NULL,
    category_type            VARCHAR(20)    NOT NULL DEFAULT 'EXPENSE',

    allocation_type          VARCHAR(20)    NOT NULL,
    constraint_role          VARCHAR(20)    NOT NULL,
    priority                 VARCHAR(20),
    current_limit_amount     NUMERIC(19, 4) NOT NULL,
    minimum_amount           NUMERIC(19, 4) NOT NULL,
    source_keys              JSONB          NOT NULL DEFAULT '[]'::jsonb,

    CONSTRAINT uq_budget_category_constraints_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_budget_category_constraints_run_scope
        UNIQUE (id, user_id, constraint_revision_id, category_id),
    CONSTRAINT uq_budget_category_constraints_category
        UNIQUE (constraint_revision_id, category_id),
    CONSTRAINT fk_budget_category_constraints_revision_user
        FOREIGN KEY (constraint_revision_id, user_id)
            REFERENCES keeper.budget_constraint_revisions (id, user_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_budget_category_constraints_category_user
        FOREIGN KEY (category_id, user_id, category_type)
            REFERENCES keeper.categories (id, user_id, type),

    CONSTRAINT chk_budget_category_constraints_category_type
        CHECK (category_type = 'EXPENSE'),
    CONSTRAINT chk_budget_category_constraints_allocation_type
        CHECK (allocation_type IN ('FIXED', 'VARIABLE')),
    CONSTRAINT chk_budget_category_constraints_role
        CHECK (constraint_role IN ('REQUIRED', 'FLEXIBLE')),
    CONSTRAINT chk_budget_category_constraints_priority
        CHECK (priority IS NULL OR priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_budget_category_constraints_amounts
        CHECK (current_limit_amount >= 0 AND minimum_amount >= 0),
    CONSTRAINT chk_budget_category_constraints_mckp_shape
        CHECK (
            (allocation_type = 'FIXED'
                AND constraint_role = 'REQUIRED'
                AND priority IS NULL)
                OR
            (allocation_type = 'VARIABLE' AND priority IS NOT NULL)
        ),
    CONSTRAINT chk_budget_category_constraints_sources_array
        CHECK (jsonb_typeof(source_keys) = 'array')
);

CREATE INDEX ix_budget_category_constraints_user_category
    ON keeper.budget_category_constraints (user_id, category_id);

--rollback DROP TABLE keeper.budget_category_constraints;

--changeset dchet:260915-1222-create-budget-constraint-funding-levels
CREATE TABLE keeper.budget_constraint_funding_levels
(
    id                       UUID PRIMARY KEY,
    user_id                  UUID           NOT NULL,
    category_constraint_id   UUID           NOT NULL,

    level                    VARCHAR(20)    NOT NULL,
    amount                   NUMERIC(19, 4) NOT NULL,
    coverage                 NUMERIC(7, 6)  NOT NULL,

    CONSTRAINT uq_budget_funding_levels_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_budget_funding_levels_run_scope
        UNIQUE (id, user_id, category_constraint_id),
    CONSTRAINT uq_budget_funding_levels_category_level
        UNIQUE (category_constraint_id, level),
    CONSTRAINT fk_budget_funding_levels_constraint_user
        FOREIGN KEY (category_constraint_id, user_id)
            REFERENCES keeper.budget_category_constraints (id, user_id)
            ON DELETE CASCADE,

    CONSTRAINT chk_budget_funding_levels_level
        CHECK (level IN ('MINIMUM', 'BALANCED', 'COMFORTABLE')),
    CONSTRAINT chk_budget_funding_levels_amount
        CHECK (amount >= 0),
    CONSTRAINT chk_budget_funding_levels_coverage
        CHECK (
            (level = 'MINIMUM' AND coverage = 0.600000)
                OR
            (level = 'BALANCED' AND coverage = 0.850000)
                OR
            (level = 'COMFORTABLE' AND coverage = 1.000000)
        )
);

COMMENT ON TABLE keeper.budget_constraint_funding_levels IS
    'Discrete MCKP options for VARIABLE categories; FIXED categories have no option rows.';

--rollback DROP TABLE keeper.budget_constraint_funding_levels;
