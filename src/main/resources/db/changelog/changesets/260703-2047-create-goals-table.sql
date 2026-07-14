--liquibase formatted sql

--changeset dasemenov:260703-2047-create-goals-table
CREATE TABLE keeper.goals
(
    id            UUID PRIMARY KEY,
    user_id       UUID           NOT NULL REFERENCES keeper.users (id),

    name          VARCHAR(150)   NOT NULL,
    target_amount NUMERIC(19, 4) NOT NULL,
    currency      VARCHAR(3)     NOT NULL,

    deadline      DATE,
    status        VARCHAR(20)    NOT NULL,
    achieved_at   TIMESTAMPTZ,
    archived_at   TIMESTAMPTZ,

    CONSTRAINT uq_goals_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_goals_id_user_currency
        UNIQUE (id, user_id, currency),
    CONSTRAINT chk_goals_name_not_blank
        CHECK (btrim(name) <> ''),
    CONSTRAINT chk_goals_target_amount_positive
        CHECK (target_amount > 0),
    CONSTRAINT chk_goals_currency
        CHECK (currency IN ('USD', 'EUR', 'RUB')),
    CONSTRAINT chk_goals_status
        CHECK (status IN ('ACTIVE', 'PAUSED', 'ACHIEVED', 'CANCELLED')),
    CONSTRAINT chk_goals_achieved_at
        CHECK (
            (status = 'ACHIEVED' AND achieved_at IS NOT NULL)
            OR (status <> 'ACHIEVED' AND achieved_at IS NULL)
        ),
    CONSTRAINT chk_goals_archived_at
        CHECK (
            (status IN ('ACHIEVED', 'CANCELLED') AND archived_at IS NOT NULL)
            OR (status IN ('ACTIVE', 'PAUSED') AND archived_at IS NULL)
        ),
    CONSTRAINT chk_goals_archive_order
        CHECK (achieved_at IS NULL OR archived_at >= achieved_at)
);

--rollback DROP TABLE keeper.goals;
