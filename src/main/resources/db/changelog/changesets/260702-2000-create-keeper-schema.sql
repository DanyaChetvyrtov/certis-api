--liquibase formatted sql

--changeset dasemenov:260702-2000-create-keeper-schema
CREATE SCHEMA keeper;

--rollback DROP SCHEMA keeper;
