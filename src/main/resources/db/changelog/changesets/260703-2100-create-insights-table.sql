-- liquibase formatted sql

-- changeset dasemenov:260703-2100-create-insights-table
CREATE TABLE IF NOT EXISTS insights
(
    id          UUID PRIMARY KEY,
    user_id     UUID REFERENCES users (id),

    type        TEXT, -- overspending | anomaly | suggestion | trend
    title       TEXT,
    description TEXT,

    severity    TEXT, -- low | medium | high

    metadata    JSONB,
    created_at  TIMESTAMP DEFAULT now()
)
-- rollback DROP TABLE insights;
