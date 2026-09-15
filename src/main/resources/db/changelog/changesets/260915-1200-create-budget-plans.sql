--liquibase formatted sql

--changeset dchet:260915-1200-create-budget-plans
CREATE TABLE keeper.budget_plans
(
    id                  UUID PRIMARY KEY,
    user_id             UUID         NOT NULL REFERENCES keeper.users (id),
    previous_plan_id    UUID,
    baseline_budget_id  UUID,
    applied_budget_id   UUID,

    budget_month        DATE         NOT NULL,
    currency            VARCHAR(3)   NOT NULL,
    revision            INTEGER      NOT NULL,
    version             BIGINT       NOT NULL DEFAULT 0,
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    idempotency_key     VARCHAR(100) NOT NULL,

    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    applied_at          TIMESTAMPTZ,
    superseded_at       TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,

    CONSTRAINT uq_budget_plans_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_budget_plans_id_user_scope
        UNIQUE (id, user_id, budget_month, currency),
    CONSTRAINT uq_budget_plans_scope_revision
        UNIQUE (user_id, budget_month, currency, revision),
    CONSTRAINT uq_budget_plans_user_idempotency
        UNIQUE (user_id, idempotency_key),

    CONSTRAINT fk_budget_plans_previous_scope
        FOREIGN KEY (previous_plan_id, user_id, budget_month, currency)
            REFERENCES keeper.budget_plans (id, user_id, budget_month, currency),
    CONSTRAINT fk_budget_plans_baseline_budget_user
        FOREIGN KEY (baseline_budget_id, user_id, budget_month, currency)
            REFERENCES keeper.budgets (id, user_id, budget_month, currency),
    CONSTRAINT fk_budget_plans_applied_budget_user
        FOREIGN KEY (applied_budget_id, user_id, budget_month, currency)
            REFERENCES keeper.budgets (id, user_id, budget_month, currency),

    CONSTRAINT chk_budget_plans_month_first_day
        CHECK (budget_month = date_trunc('month', budget_month)::date),
    CONSTRAINT chk_budget_plans_currency
        CHECK (currency IN ('USD', 'EUR', 'RUB')),
    CONSTRAINT chk_budget_plans_revision_positive
        CHECK (revision > 0),
    CONSTRAINT chk_budget_plans_version_non_negative
        CHECK (version >= 0),
    CONSTRAINT chk_budget_plans_status
        CHECK (status IN ('DRAFT', 'APPLIED', 'SUPERSEDED', 'CANCELLED')),
    CONSTRAINT chk_budget_plans_idempotency_not_blank
        CHECK (btrim(idempotency_key) <> ''),
    CONSTRAINT chk_budget_plans_previous_not_self
        CHECK (previous_plan_id IS NULL OR previous_plan_id <> id),
    CONSTRAINT chk_budget_plans_updated_at
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_budget_plans_lifecycle_timestamps
        CHECK (
            (status = 'DRAFT'
                AND applied_at IS NULL
                AND superseded_at IS NULL
                AND cancelled_at IS NULL)
                OR
            (status = 'APPLIED'
                AND applied_at IS NOT NULL
                AND superseded_at IS NULL
                AND cancelled_at IS NULL)
                OR
            (status = 'SUPERSEDED'
                AND applied_at IS NOT NULL
                AND superseded_at IS NOT NULL
                AND cancelled_at IS NULL)
                OR
            (status = 'CANCELLED'
                AND applied_at IS NULL
                AND superseded_at IS NULL
                AND cancelled_at IS NOT NULL)
        ),
    CONSTRAINT chk_budget_plans_applied_budget_state
        CHECK (
            (status IN ('APPLIED', 'SUPERSEDED') AND applied_budget_id IS NOT NULL)
                OR
            (status IN ('DRAFT', 'CANCELLED') AND applied_budget_id IS NULL)
        ),
    CONSTRAINT chk_budget_plans_event_times
        CHECK (
            (applied_at IS NULL OR applied_at >= created_at)
                AND (superseded_at IS NULL OR superseded_at >= applied_at)
                AND (cancelled_at IS NULL OR cancelled_at >= created_at)
        )
);

CREATE UNIQUE INDEX uq_budget_plans_active_draft
    ON keeper.budget_plans (user_id, budget_month, currency)
    WHERE status = 'DRAFT';

CREATE UNIQUE INDEX uq_budget_plans_current_applied
    ON keeper.budget_plans (user_id, budget_month, currency)
    WHERE status = 'APPLIED';

CREATE INDEX ix_budget_plans_scope_history
    ON keeper.budget_plans (user_id, budget_month, currency, revision DESC);

CREATE INDEX ix_budget_plans_previous
    ON keeper.budget_plans (previous_plan_id)
    WHERE previous_plan_id IS NOT NULL;

COMMENT ON TABLE keeper.budget_plans IS
    'Versioned planning sessions. UI steps are derived; only durable lifecycle states are stored.';

--rollback DROP TABLE keeper.budget_plans;
