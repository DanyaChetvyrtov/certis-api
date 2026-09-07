--liquibase formatted sql

--changeset dchet:260907-2030-extend-goals-table
ALTER TABLE keeper.goals
    ADD COLUMN contribution_plan_type VARCHAR(20),
    ADD COLUMN monthly_contribution_amount NUMERIC(19, 4),
    ADD COLUMN icon VARCHAR(50),
    ADD COLUMN color VARCHAR(7),
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE keeper.goals
SET contribution_plan_type = 'RECOMMENDED',
    monthly_contribution_amount = target_amount,
    icon = 'target',
    color = '#10B981',
    created_at = COALESCE(achieved_at, archived_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(archived_at, achieved_at, CURRENT_TIMESTAMP);

ALTER TABLE keeper.goals
    ALTER COLUMN contribution_plan_type SET NOT NULL,
    ALTER COLUMN monthly_contribution_amount SET NOT NULL,
    ALTER COLUMN icon SET NOT NULL,
    ALTER COLUMN color SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    ADD CONSTRAINT chk_goals_contribution_plan_type
        CHECK (contribution_plan_type IN ('RECOMMENDED', 'CUSTOM')),
    ADD CONSTRAINT chk_goals_monthly_contribution_amount_positive
        CHECK (monthly_contribution_amount > 0),
    ADD CONSTRAINT chk_goals_icon_not_blank
        CHECK (btrim(icon) <> ''),
    ADD CONSTRAINT chk_goals_color
        CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    ADD CONSTRAINT chk_goals_updated_at
        CHECK (updated_at >= created_at),
    ADD CONSTRAINT chk_goals_achieved_after_created
        CHECK (achieved_at IS NULL OR achieved_at >= created_at),
    ADD CONSTRAINT chk_goals_archived_after_created
        CHECK (archived_at IS NULL OR archived_at >= created_at);

CREATE INDEX ix_goals_user_currency_status_deadline
    ON keeper.goals (user_id, currency, status, deadline);

--rollback DROP INDEX keeper.ix_goals_user_currency_status_deadline; ALTER TABLE keeper.goals DROP CONSTRAINT chk_goals_archived_after_created, DROP CONSTRAINT chk_goals_achieved_after_created, DROP CONSTRAINT chk_goals_updated_at, DROP CONSTRAINT chk_goals_color, DROP CONSTRAINT chk_goals_icon_not_blank, DROP CONSTRAINT chk_goals_monthly_contribution_amount_positive, DROP CONSTRAINT chk_goals_contribution_plan_type, DROP COLUMN updated_at, DROP COLUMN created_at, DROP COLUMN color, DROP COLUMN icon, DROP COLUMN monthly_contribution_amount, DROP COLUMN contribution_plan_type;

--changeset dchet:260907-2031-extend-goal-transactions-table
ALTER TABLE keeper.goal_transactions
    ADD COLUMN reversal_of_goal_transaction_id UUID,
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD COLUMN note TEXT,
    ADD CONSTRAINT uq_goal_transactions_reversal_target
        UNIQUE (id, user_id, goal_id, account_id, currency);

ALTER TABLE keeper.goal_transactions
    ADD CONSTRAINT fk_goal_transactions_reversal_target
        FOREIGN KEY (
            reversal_of_goal_transaction_id,
            user_id,
            goal_id,
            account_id,
            currency
        )
            REFERENCES keeper.goal_transactions (
                id,
                user_id,
                goal_id,
                account_id,
                currency
            ),
    ADD CONSTRAINT chk_goal_transactions_reversal_not_self
        CHECK (reversal_of_goal_transaction_id IS NULL OR reversal_of_goal_transaction_id <> id),
    ADD CONSTRAINT chk_goal_transactions_reversal_type
        CHECK (reversal_of_goal_transaction_id IS NULL OR type = 'REFUND'),
    ADD CONSTRAINT chk_goal_transactions_idempotency_key_not_blank
        CHECK (idempotency_key IS NULL OR btrim(idempotency_key) <> ''),
    ADD CONSTRAINT chk_goal_transactions_note_not_blank
        CHECK (note IS NULL OR btrim(note) <> '');

CREATE UNIQUE INDEX uq_goal_transactions_idempotency_key
    ON keeper.goal_transactions (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX uq_goal_transactions_reversal
    ON keeper.goal_transactions (reversal_of_goal_transaction_id)
    WHERE reversal_of_goal_transaction_id IS NOT NULL;

CREATE INDEX ix_goal_transactions_user_goal_date
    ON keeper.goal_transactions (user_id, goal_id, date DESC);

CREATE INDEX ix_goal_transactions_user_account_date
    ON keeper.goal_transactions (user_id, account_id, date DESC);

CREATE INDEX ix_goal_transactions_user_currency_date
    ON keeper.goal_transactions (user_id, currency, date DESC);

--rollback DROP INDEX keeper.ix_goal_transactions_user_currency_date; DROP INDEX keeper.ix_goal_transactions_user_account_date; DROP INDEX keeper.ix_goal_transactions_user_goal_date; DROP INDEX keeper.uq_goal_transactions_reversal; DROP INDEX keeper.uq_goal_transactions_idempotency_key; ALTER TABLE keeper.goal_transactions DROP CONSTRAINT chk_goal_transactions_note_not_blank, DROP CONSTRAINT chk_goal_transactions_idempotency_key_not_blank, DROP CONSTRAINT chk_goal_transactions_reversal_type, DROP CONSTRAINT chk_goal_transactions_reversal_not_self, DROP CONSTRAINT fk_goal_transactions_reversal_target, DROP CONSTRAINT uq_goal_transactions_reversal_target, DROP COLUMN note, DROP COLUMN idempotency_key, DROP COLUMN reversal_of_goal_transaction_id;
