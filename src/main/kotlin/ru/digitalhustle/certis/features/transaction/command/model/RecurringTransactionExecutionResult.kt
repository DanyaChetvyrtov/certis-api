package ru.digitalhustle.certis.features.transaction.command.model

import java.time.LocalDate
import java.util.UUID

data class RecurringTransactionExecutionResult(
    val templateId: UUID,
    val scheduledFor: LocalDate,
)
