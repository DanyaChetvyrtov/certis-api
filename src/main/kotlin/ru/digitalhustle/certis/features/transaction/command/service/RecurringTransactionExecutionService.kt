package ru.digitalhustle.certis.features.transaction.command.service

import ru.digitalhustle.certis.features.transaction.command.model.RecurringTransactionExecutionResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface RecurringTransactionExecutionService {

    fun executeNext(
        currentDate: LocalDate,
        currentTime: OffsetDateTime,
        excludedTemplateIds: Set<UUID>,
    ): RecurringTransactionExecutionResult?

    fun execute(
        templateId: UUID,
        currentDate: LocalDate,
    )
}
