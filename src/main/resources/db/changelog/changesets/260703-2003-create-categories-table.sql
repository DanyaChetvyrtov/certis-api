--liquibase formatted sql

--changeset dasemenov:260703-2003-create-categories-table
CREATE TABLE keeper.categories
(
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES keeper.users (id),
    name        VARCHAR(150) NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    icon        VARCHAR(50)  NOT NULL,
    color       VARCHAR(7)   NOT NULL,
    archived_at TIMESTAMPTZ,

    CONSTRAINT uq_categories_id_user
        UNIQUE (id, user_id),
    CONSTRAINT uq_categories_id_user_type
        UNIQUE (id, user_id, type),
    CONSTRAINT chk_categories_name_not_blank
        CHECK (btrim(name) <> ''),
    CONSTRAINT chk_categories_type
        CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_categories_icon_not_blank
        CHECK (btrim(icon) <> ''),
    CONSTRAINT chk_categories_color
        CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE UNIQUE INDEX uq_categories_active_user_type_name
    ON keeper.categories (
                          user_id,
                          type,
                          lower(btrim(name))
        )
    WHERE archived_at IS NULL;

--rollback DROP TABLE keeper.categories;
