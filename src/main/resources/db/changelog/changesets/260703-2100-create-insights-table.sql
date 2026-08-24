--liquibase formatted sql

--changeset dasemenov:260703-2100-create-insights-table
CREATE TABLE keeper.insights
(
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES keeper.users (id),

    type        VARCHAR(30)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT         NOT NULL,

    severity    VARCHAR(20)  NOT NULL,

    metadata    JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_insights_type
        CHECK (type IN ('OVERSPENDING', 'ANOMALY', 'SUGGESTION', 'TREND')),
    CONSTRAINT chk_insights_title_not_blank
        CHECK (btrim(title) <> ''),
    CONSTRAINT chk_insights_description_not_blank
        CHECK (btrim(description) <> ''),
    CONSTRAINT chk_insights_severity
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_insights_metadata_object
        CHECK (metadata IS NULL OR jsonb_typeof(metadata) = 'object')
);

--rollback DROP TABLE keeper.insights;
