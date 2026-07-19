-- liquibase formatted sql

-- changeset dasemenov:260703-2047-create-goals-table
CREATE TABLE IF NOT EXISTS keeper.goals
(
    id             UUID PRIMARY KEY,
    user_id        UUID REFERENCES keeper.users (id),

    name           TEXT NOT NULL,
    target_amount  NUMERIC(14, 2) NOT NULL,
    current_amount NUMERIC(14, 2) NOT NULL,

    deadline       DATE,
    status         VARCHAR(50)
)
-- rollback DROP TABLE goals;
