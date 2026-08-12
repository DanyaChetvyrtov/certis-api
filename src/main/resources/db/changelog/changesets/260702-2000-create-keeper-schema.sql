--liquibase formatted sql

--changeset dasemenov:260702-2000-create-keeper-schema
CREATE SCHEMA IF NOT EXISTS keeper;

--rollback DROP SCHEMA keeper;
