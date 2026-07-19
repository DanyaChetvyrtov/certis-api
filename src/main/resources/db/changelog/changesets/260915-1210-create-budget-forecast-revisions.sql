--liquibase formatted sql

--changeset dchet:260915-1210-create-budget-forecast-revisions
CREATE TABLE keeper.budget_forecast_revisions
(
    id                       UUID PRIMARY KEY,
    user_id                  UUID           NOT NULL,
    plan_id                  UUID           NOT NULL,
    revision                 INTEGER        NOT NULL,

    source_fingerprint       VARCHAR(71)    NOT NULL,
    forecast_fingerprint     VARCHAR(71)    NOT NULL,
    history_months_used      SMALLINT       NOT NULL,
    history_method           VARCHAR(50)    NOT NULL,

    forecast_income          NUMERIC(19, 4) NOT NULL,
    recurring_income         NUMERIC(19, 4) NOT NULL,
    recurring_expenses       NUMERIC(19, 4) NOT NULL,
    flexible_estimate        NUMERIC(19, 4) NOT NULL,
    forecast_expenses        NUMERIC(19, 4) NOT NULL,
    forecast_savings         NUMERIC(19, 4) NOT NULL,

    source_snapshot          JSONB          NOT NULL,
    confirmed_at             TIMESTAMPTZ    NOT NULL,

    CONSTRAINT uq_budget_forecasts_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_budget_forecasts_id_user_plan
        UNIQUE (id, user_id, plan_id),
    CONSTRAINT uq_budget_forecasts_plan_revision
        UNIQUE (plan_id, revision),
    CONSTRAINT fk_budget_forecasts_plan_user
        FOREIGN KEY (plan_id, user_id)
            REFERENCES keeper.budget_plans (id, user_id)
            ON DELETE CASCADE,

    CONSTRAINT chk_budget_forecasts_revision_positive
        CHECK (revision > 0),
    CONSTRAINT chk_budget_forecasts_source_hash
        CHECK (source_fingerprint ~ '^sha256:[0-9a-f]{64}$'),
    CONSTRAINT chk_budget_forecasts_forecast_hash
        CHECK (forecast_fingerprint ~ '^sha256:[0-9a-f]{64}$'),
    CONSTRAINT chk_budget_forecasts_history_months
        CHECK (history_months_used >= 0),
    CONSTRAINT chk_budget_forecasts_history_method
        CHECK (btrim(history_method) <> ''),
    CONSTRAINT chk_budget_forecasts_amounts
        CHECK (
            forecast_income >= 0
                AND recurring_income >= 0
                AND recurring_income <= forecast_income
                AND recurring_expenses >= 0
                AND flexible_estimate >= 0
                AND forecast_expenses >= 0
        ),
    CONSTRAINT chk_budget_forecasts_expense_total
        CHECK (forecast_expenses = recurring_expenses + flexible_estimate),
    CONSTRAINT chk_budget_forecasts_savings_total
        CHECK (forecast_savings = forecast_income - forecast_expenses),
    CONSTRAINT chk_budget_forecasts_source_object
        CHECK (jsonb_typeof(source_snapshot) = 'object')
);

CREATE INDEX ix_budget_forecasts_plan_latest
    ON keeper.budget_forecast_revisions (plan_id, revision DESC);

COMMENT ON TABLE keeper.budget_forecast_revisions IS
    'Immutable confirmed forecast snapshots used by constraint and optimization revisions.';

--rollback DROP TABLE keeper.budget_forecast_revisions;

--changeset dchet:260915-1211-create-budget-forecast-items
CREATE TABLE keeper.budget_forecast_items
(
    id                                UUID PRIMARY KEY,
    user_id                           UUID           NOT NULL,
    forecast_revision_id              UUID           NOT NULL,
    category_id                       UUID,
    category_type                     VARCHAR(20),

    source_key                        VARCHAR(255)   NOT NULL,
    source_type                       VARCHAR(30)    NOT NULL,
    operation_type                    VARCHAR(20)    NOT NULL,
    title                             VARCHAR(255)   NOT NULL,
    included                          BOOLEAN        NOT NULL,
    constraint_role                   VARCHAR(20),
    confidence                        VARCHAR(20)    NOT NULL,

    original_amount                   NUMERIC(19, 4) NOT NULL,
    effective_amount                  NUMERIC(19, 4) NOT NULL,
    scheduled_for                     DATE,

    recurring_transaction_template_id UUID,
    transaction_id                    UUID,
    manual_client_id                  UUID,
    source_updated_at                 TIMESTAMPTZ,
    history_months_used               SMALLINT,
    history_method                    VARCHAR(50),
    source_payload                    JSONB          NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT uq_budget_forecast_items_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_budget_forecast_items_revision_source
        UNIQUE (forecast_revision_id, source_key),
    CONSTRAINT fk_budget_forecast_items_revision_user
        FOREIGN KEY (forecast_revision_id, user_id)
            REFERENCES keeper.budget_forecast_revisions (id, user_id)
            ON DELETE CASCADE,
    CONSTRAINT fk_budget_forecast_items_category_user_type
        FOREIGN KEY (category_id, user_id, category_type)
            REFERENCES keeper.categories (id, user_id, type),

    CONSTRAINT chk_budget_forecast_items_source_key
        CHECK (btrim(source_key) <> ''),
    CONSTRAINT chk_budget_forecast_items_source_type
        CHECK (source_type IN (
            'RECURRING',
            'HISTORICAL_CATEGORY',
            'CURRENT_LIMIT',
            'MANUAL',
            'ACTUAL'
        )),
    CONSTRAINT chk_budget_forecast_items_operation_type
        CHECK (operation_type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_budget_forecast_items_title
        CHECK (btrim(title) <> ''),
    CONSTRAINT chk_budget_forecast_items_role
        CHECK (
            (operation_type = 'INCOME' AND constraint_role IS NULL)
                OR
            (operation_type = 'EXPENSE'
                AND constraint_role IS NOT NULL
                AND constraint_role IN ('REQUIRED', 'FLEXIBLE'))
        ),
    CONSTRAINT chk_budget_forecast_items_confidence
        CHECK (confidence IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_budget_forecast_items_amounts
        CHECK (original_amount > 0 AND effective_amount > 0),
    CONSTRAINT chk_budget_forecast_items_category_pair
        CHECK (
            (category_id IS NULL AND category_type IS NULL)
                OR
            (category_id IS NOT NULL
                AND category_type IS NOT NULL
                AND category_type = operation_type)
        ),
    CONSTRAINT chk_budget_forecast_items_source_identity
        CHECK (
            (source_type = 'RECURRING'
                AND recurring_transaction_template_id IS NOT NULL
                AND scheduled_for IS NOT NULL)
                OR
            (source_type = 'HISTORICAL_CATEGORY'
                AND category_id IS NOT NULL
                AND history_months_used IS NOT NULL
                AND history_months_used > 0
                AND history_method IS NOT NULL
                AND btrim(history_method) <> '')
                OR
            (source_type = 'CURRENT_LIMIT' AND category_id IS NOT NULL)
                OR
            (source_type = 'MANUAL' AND manual_client_id IS NOT NULL)
                OR
            (source_type = 'ACTUAL' AND transaction_id IS NOT NULL)
        ),
    CONSTRAINT chk_budget_forecast_items_history_months
        CHECK (history_months_used IS NULL OR history_months_used > 0),
    CONSTRAINT chk_budget_forecast_items_history_method
        CHECK (history_method IS NULL OR btrim(history_method) <> ''),
    CONSTRAINT chk_budget_forecast_items_source_payload
        CHECK (jsonb_typeof(source_payload) = 'object')
);

CREATE INDEX ix_budget_forecast_items_revision_type
    ON keeper.budget_forecast_items (forecast_revision_id, source_type);

CREATE INDEX ix_budget_forecast_items_user_category
    ON keeper.budget_forecast_items (user_id, category_id)
    WHERE category_id IS NOT NULL;

COMMENT ON COLUMN keeper.budget_forecast_items.recurring_transaction_template_id IS
    'Snapshot reference only; deliberately not a foreign key so planning history survives source deletion.';

COMMENT ON COLUMN keeper.budget_forecast_items.transaction_id IS
    'Snapshot reference only; deliberately not a foreign key so planning history survives source deletion.';

--rollback DROP TABLE keeper.budget_forecast_items;
