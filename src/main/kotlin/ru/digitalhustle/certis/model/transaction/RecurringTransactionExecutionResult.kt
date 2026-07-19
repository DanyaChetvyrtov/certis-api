package ru.digitalhustle.certis.model.transaction

import java.time.LocalDate
import java.util.UUID

data class RecurringTransactionExecutionResult(
    val templateId: UUID,
    val scheduledFor: LocalDate,
)
