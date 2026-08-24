--liquibase formatted sql

--changeset dasemenov:260714-1442-create-profiles-table
CREATE TABLE keeper.profiles
(
    id            UUID PRIMARY KEY REFERENCES keeper.users (id),
    name          VARCHAR(100) NOT NULL,
    surname       VARCHAR(100) NOT NULL,
    date_of_birth DATE         NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT chk_profiles_name_not_blank
        CHECK (btrim(name) <> ''),
    CONSTRAINT chk_profiles_surname_not_blank
        CHECK (btrim(surname) <> ''),
    CONSTRAINT chk_profiles_date_of_birth_in_past
        CHECK (date_of_birth < CURRENT_DATE)
);

--rollback DROP TABLE keeper.profiles;
