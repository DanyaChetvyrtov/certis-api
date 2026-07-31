--liquibase formatted sql

--changeset dasemenov:260703-2052-create-goal-transactions-table
CREATE TABLE keeper.goal_transactions
(
    id         UUID PRIMARY KEY,
    user_id    UUID           NOT NULL,
    goal_id    UUID           NOT NULL,
    account_id UUID           NOT NULL,
    currency   VARCHAR(3)     NOT NULL,

    type       VARCHAR(20)    NOT NULL,
    amount     NUMERIC(19, 4) NOT NULL,

    date       TIMESTAMPTZ    NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL,

    CONSTRAINT fk_goal_transactions_goal_user
        FOREIGN KEY (goal_id, user_id, currency)
            REFERENCES keeper.goals (id, user_id, currency),
    CONSTRAINT fk_goal_transactions_account_user
        FOREIGN KEY (account_id, user_id, currency)
            REFERENCES keeper.accounts (id, user_id, currency),
    CONSTRAINT chk_goal_transactions_type
        CHECK (type IN ('CONTRIBUTION', 'REFUND')),
    CONSTRAINT chk_goal_transactions_amount_positive
        CHECK (amount > 0)
);

--rollback DROP TABLE keeper.goal_transactions;
