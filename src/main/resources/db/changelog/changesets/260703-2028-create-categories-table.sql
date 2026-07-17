-- liquibase formatted sql

-- changeset dasemenov:260703-2028-create-categories-table
CREATE TABLE IF NOT EXISTS keeper.categories
(
    id        UUID PRIMARY KEY,
    user_id   UUID REFERENCES keeper.users (id),

    name      VARCHAR(150) NOT NULL,
    type      VARCHAR(100) NOT NULL,
    icon      TEXT NOT NULL,
    color     VARCHAR(7) NOT NULL,

    parent_id UUID NULL REFERENCES keeper.categories (id)
)
-- rollback DROP TABLE categories;
