package ru.digitalhustle.certis.features.transaction.command.model

import java.time.OffsetDateTime

data class RecurringTransactionRetryState(
    val consecutiveFailures: Int,
    val retryAfter: OffsetDateTime,
)
