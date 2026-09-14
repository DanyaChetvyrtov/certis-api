package ru.digitalhustle.certis.features.transaction.constants

object TransactionErrorMessages {

    const val TRANSACTION_ACCOUNT_CLOSED = "Closed account cannot be used for a new transaction"
    const val TRANSACTION_CATEGORY_ARCHIVED = "Archived category cannot be assigned to a transaction"
    const val TRANSACTION_ALREADY_CATEGORIZED = "Only uncategorized transactions can be assigned a category"
    const val TRANSACTION_CATEGORY_TYPE_MISMATCH = "Transaction and category types must match"
    const val TRANSACTION_CATEGORY_ASSIGNMENT_FAILED = "Transaction category assignment could not be completed"
    const val TRANSACTION_DUPLICATE_CATEGORY_ASSIGNMENTS = "Each transaction can be assigned only once per request"

    const val TRANSFER_SAME_ACCOUNT = "Source and destination accounts must be different"
    const val TRANSFER_ACCOUNT_CLOSED = "Closed account cannot be used for a transfer"
    const val TRANSFER_CURRENCY_MISMATCH = "Source and destination account currencies must match"
    const val TRANSFER_REVERSAL_OF_REVERSAL = "A transfer reversal cannot be reversed"
    const val TRANSFER_TRANSACTION_IMMUTABLE = "Transfer transactions cannot be modified independently"

    const val RECURRING_TRANSACTION_STATUS = "Recurring transaction status must be ACTIVE or PAUSED"
    const val RECURRING_TRANSACTION_TERMINAL = "Completed or cancelled recurring transaction cannot be updated"
    const val RECURRING_TRANSACTION_DATE_RANGE = "End date must not be before start date or last run date"
}
