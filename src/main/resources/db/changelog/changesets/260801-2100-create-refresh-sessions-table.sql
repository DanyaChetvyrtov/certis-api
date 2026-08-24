--liquibase formatted sql

--changeset dchetvertov:260801-2100-create-refresh-sessions-table
CREATE TABLE keeper.refresh_sessions
(
    id         UUID PRIMARY KEY,
    family_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL REFERENCES keeper.users (id) ON DELETE RESTRICT,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_refresh_sessions_expiration
        CHECK (expires_at > created_at),
    CONSTRAINT chk_refresh_sessions_used_at
        CHECK (used_at IS NULL OR used_at >= created_at),
    CONSTRAINT chk_refresh_sessions_revoked_at
        CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX idx_refresh_sessions_family_id
    ON keeper.refresh_sessions (family_id);

CREATE INDEX idx_refresh_sessions_user_id
    ON keeper.refresh_sessions (user_id);

--rollback DROP TABLE keeper.refresh_sessions;
