package ru.digitalhustle.certis.exception.custom

import java.time.LocalDate
import java.util.UUID

class RecurringTransactionExecutionException(
    val templateId: UUID,
    val scheduledFor: LocalDate,
    cause: RuntimeException,
) : RuntimeException("Failed to execute recurring transaction template $templateId", cause)
