package ru.digitalhustle.certis.model.transaction

import java.time.OffsetDateTime

data class RecurringTransactionRetryState(
    val consecutiveFailures: Int,
    val retryAfter: OffsetDateTime,
)
