-- liquibase formatted sql

-- changeset dasemenov:260703-2052-create-goal-contributions-table
CREATE TABLE IF NOT EXISTS goal_contributions
(
    id             UUID PRIMARY KEY,
    goal_id        UUID REFERENCES goals (id),
    transaction_id UUID REFERENCES transactions (id),
    amount         NUMERIC(14, 2) NOT NULL
)
-- rollback DROP TABLE goals;
