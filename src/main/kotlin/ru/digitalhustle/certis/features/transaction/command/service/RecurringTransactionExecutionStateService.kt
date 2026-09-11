package ru.digitalhustle.certis.features.transaction.command.service

import ru.digitalhustle.certis.features.transaction.command.model.RecurringTransactionRetryState
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface RecurringTransactionExecutionStateService {

    fun getByIdForUpdate(id: UUID): RecurringTransactionTemplate

    fun findDueForUpdate(
        currentDate: LocalDate,
        currentTime: OffsetDateTime,
        excludedTemplateIds: Set<UUID>,
    ): RecurringTransactionTemplate?

    fun recordExecution(
        template: RecurringTransactionTemplate,
        lastRunDate: LocalDate,
        nextRunDate: LocalDate?,
    ): RecurringTransactionTemplate

    fun recordExecutionFailure(
        id: UUID,
        scheduledFor: LocalDate,
    ): RecurringTransactionRetryState?
}
