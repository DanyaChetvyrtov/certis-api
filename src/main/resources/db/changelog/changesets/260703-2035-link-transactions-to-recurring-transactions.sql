--liquibase formatted sql

--changeset dasemenov:260703-2035-link-transactions-to-recurring-transactions
ALTER TABLE keeper.transactions
    ADD COLUMN recurring_transaction_id UUID,
    ADD CONSTRAINT fk_transactions_recurring_transaction_user
        FOREIGN KEY (recurring_transaction_id, user_id)
            REFERENCES keeper.recurring_transactions (id, user_id);

--rollback ALTER TABLE keeper.transactions DROP CONSTRAINT fk_transactions_recurring_transaction_user;
--rollback ALTER TABLE keeper.transactions DROP COLUMN recurring_transaction_id;
