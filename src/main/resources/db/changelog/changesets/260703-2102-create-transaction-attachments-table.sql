-- liquibase formatted sql

-- changeset dasemenov:260703-2102-create-transaction-attachments-table
CREATE TABLE IF NOT EXISTS transaction_attachments
(
    id             UUID PRIMARY KEY,
    transaction_id UUID REFERENCES transactions (id),
    file_url       TEXT,
    file_type      TEXT
)
-- rollback DROP TABLE transaction_attachments;
