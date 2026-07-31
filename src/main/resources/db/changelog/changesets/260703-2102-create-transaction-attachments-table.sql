--liquibase formatted sql

--changeset dasemenov:260703-2102-create-transaction-attachments-table
CREATE TABLE keeper.transaction_attachments
(
    id             UUID PRIMARY KEY,
    transaction_id UUID         NOT NULL REFERENCES keeper.transactions (id),
    file_url       TEXT         NOT NULL,
    file_type      VARCHAR(100) NOT NULL,

    CONSTRAINT chk_transaction_attachments_file_url_not_blank
        CHECK (btrim(file_url) <> ''),
    CONSTRAINT chk_transaction_attachments_file_type_not_blank
        CHECK (btrim(file_type) <> '')
);

--rollback DROP TABLE keeper.transaction_attachments;
