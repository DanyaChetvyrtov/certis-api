--liquibase formatted sql

--changeset dasemenov:260816-1500-create-transfers-table
CREATE TABLE keeper.transfers
(
    id                      UUID PRIMARY KEY,

    user_id                 UUID           NOT NULL,
    source_account_id       UUID           NOT NULL,
    destination_account_id  UUID           NOT NULL,
    reversal_of_transfer_id UUID,
    currency                VARCHAR(3)     NOT NULL,

    amount                  NUMERIC(19, 4) NOT NULL,
    note                    TEXT,

    occurred_at             TIMESTAMPTZ    NOT NULL,
    created_at              TIMESTAMPTZ    NOT NULL,


    CONSTRAINT fk_transfers_source_account_user_currency
        FOREIGN KEY (source_account_id, user_id, currency)
            REFERENCES keeper.accounts (id, user_id, currency),
    CONSTRAINT fk_transfers_destination_account_user_currency
        FOREIGN KEY (destination_account_id, user_id, currency)
            REFERENCES keeper.accounts (id, user_id, currency),
    CONSTRAINT fk_transfers_reversal_user
        FOREIGN KEY (reversal_of_transfer_id, user_id)
            REFERENCES keeper.transfers (id, user_id),

    CONSTRAINT uq_transfers_id_user
        UNIQUE (id, user_id),

    CONSTRAINT chk_transfers_different_accounts
        CHECK (source_account_id <> destination_account_id),
    CONSTRAINT chk_transfers_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_transfers_reversal_not_self
        CHECK (reversal_of_transfer_id IS NULL OR reversal_of_transfer_id <> id)
);

CREATE INDEX ix_transfers_user_occurred_at
    ON keeper.transfers (user_id, occurred_at DESC);

CREATE INDEX ix_transfers_user_source_account_occurred_at
    ON keeper.transfers (user_id, source_account_id, occurred_at DESC);

CREATE INDEX ix_transfers_user_destination_account_occurred_at
    ON keeper.transfers (user_id, destination_account_id, occurred_at DESC);

ALTER TABLE keeper.transactions
    ADD COLUMN transfer_id UUID,
    ADD CONSTRAINT fk_transactions_transfer_user
        FOREIGN KEY (transfer_id, user_id)
            REFERENCES keeper.transfers (id, user_id),
    ADD CONSTRAINT chk_transactions_transfer_origin
        CHECK (
            transfer_id IS NULL
                OR
            (
                category_id IS NULL
                    AND recurring_transaction_template_id IS NULL
                    AND scheduled_for IS NULL
                )
            );

CREATE UNIQUE INDEX uq_transactions_transfer_type
    ON keeper.transactions (transfer_id, type)
    WHERE transfer_id IS NOT NULL;

CREATE UNIQUE INDEX uq_transfers_reversal
    ON keeper.transfers (reversal_of_transfer_id)
    WHERE reversal_of_transfer_id IS NOT NULL;

--rollback ALTER TABLE keeper.transactions DROP COLUMN transfer_id; DROP TABLE keeper.transfers;
