package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.config.properties.RecurringTransactionProperties
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.RecurringTransactionRetryState
import ru.digitalhustle.certis.repository.RecurringTransactionTemplateRepository
import ru.digitalhustle.certis.service.domain.RecurringTransactionExecutionStateService
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class RecurringTransactionExecutionStateServiceImpl(
    private val recurringTransactionTemplateRepository: RecurringTransactionTemplateRepository,
    private val clock: Clock,
    private val properties: RecurringTransactionProperties,
) : RecurringTransactionExecutionStateService {

    override fun getByIdForUpdate(id: UUID): RecurringTransactionTemplate =
        recurringTransactionTemplateRepository.findByIdForUpdate(id)
            ?: throw NotFoundException.entity(ENTITY_NAME)

    override fun findDueForUpdate(
        currentDate: LocalDate,
        currentTime: OffsetDateTime,
        excludedTemplateIds: Set<UUID>,
    ): RecurringTransactionTemplate? = recurringTransactionTemplateRepository.findDueForUpdate(
        currentDate = currentDate,
        currentTime = currentTime,
        excludedTemplateIds = excludedTemplateIds,
    )

    override fun recordExecution(
        template: RecurringTransactionTemplate,
        lastRunDate: LocalDate,
        nextRunDate: LocalDate?,
    ): RecurringTransactionTemplate =
        saveUpdated(
            template.copy(
                status = if (nextRunDate == null) {
                    RecurringTransactionTemplateStatus.COMPLETED
                } else {
                    RecurringTransactionTemplateStatus.ACTIVE
                },
                lastRunDate = lastRunDate,
                nextRunDate = nextRunDate,
                updatedAt = OffsetDateTime.now(clock),
            ),
        )

    @Transactional
    override fun recordExecutionFailure(
        id: UUID,
        scheduledFor: LocalDate,
    ): RecurringTransactionRetryState? {
        val currentFailures = recurringTransactionTemplateRepository.findFailureCountForUpdate(id, scheduledFor)
            ?: return null
        val consecutiveFailures = currentFailures + 1
        val retryAfter = OffsetDateTime.now(clock).plus(calculateRetryDelay(currentFailures))
        val recorded = recurringTransactionTemplateRepository.recordExecutionFailure(
            id = id,
            scheduledFor = scheduledFor,
            consecutiveFailures = consecutiveFailures,
            retryAfter = retryAfter,
        )

        return if (recorded) {
            RecurringTransactionRetryState(consecutiveFailures, retryAfter)
        } else {
            null
        }
    }

    private fun saveUpdated(template: RecurringTransactionTemplate): RecurringTransactionTemplate =
        recurringTransactionTemplateRepository.update(template)
            ?: throw NotFoundException.entity(ENTITY_NAME)

    private fun calculateRetryDelay(currentFailures: Int): Duration {
        val maxDelay = properties.scheduler.retryMaxDelay
        var delay = properties.scheduler.retryInitialDelay

        repeat(currentFailures.coerceAtMost(MAX_BACKOFF_DOUBLINGS)) {
            if (delay > maxDelay.dividedBy(2)) {
                return maxDelay
            }
            delay = delay.multipliedBy(2)
        }

        return delay.coerceAtMost(maxDelay)
    }

    private companion object {
        private const val ENTITY_NAME = "Recurring transaction"
        private const val MAX_BACKOFF_DOUBLINGS = 62
    }
}
