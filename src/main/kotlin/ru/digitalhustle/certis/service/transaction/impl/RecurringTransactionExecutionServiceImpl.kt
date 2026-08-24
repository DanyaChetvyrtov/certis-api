package ru.digitalhustle.certis.service.transaction.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.exception.custom.RecurringTransactionExecutionException
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.RecurringTransactionExecutionResult
import ru.digitalhustle.certis.provider.RecurringTransactionScheduleProvider
import ru.digitalhustle.certis.service.domain.RecurringTransactionExecutionStateService
import ru.digitalhustle.certis.service.domain.TransactionService
import ru.digitalhustle.certis.service.transaction.RecurringTransactionExecutionService
import ru.digitalhustle.certis.util.validation.AccountValidator
import ru.digitalhustle.certis.util.validation.CategoryValidator
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class RecurringTransactionExecutionServiceImpl(
    private val executionStateService: RecurringTransactionExecutionStateService,
    private val transactionService: TransactionService,
    private val scheduleProvider: RecurringTransactionScheduleProvider,
    private val accountValidator: AccountValidator,
    private val categoryValidator: CategoryValidator,
) : RecurringTransactionExecutionService {

    @Transactional
    override fun executeNext(
        currentDate: LocalDate,
        currentTime: OffsetDateTime,
        excludedTemplateIds: Set<UUID>,
    ): RecurringTransactionExecutionResult? {
        val template = executionStateService.findDueForUpdate(
            currentDate = currentDate,
            currentTime = currentTime,
            excludedTemplateIds = excludedTemplateIds,
        ) ?: return null
        val scheduledFor = checkNotNull(template.nextRunDate)

        return try {
            checkNotNull(executeLocked(template, currentDate)) {
                "Claimed recurring transaction template is no longer executable"
            }
        } catch (exception: RuntimeException) {
            throw RecurringTransactionExecutionException(template.id, scheduledFor, exception)
        }
    }

    @Transactional
    override fun execute(
        templateId: UUID,
        currentDate: LocalDate,
    ) {
        val template = executionStateService.getByIdForUpdate(templateId)
        executeLocked(template, currentDate)
    }

    private fun executeLocked(
        template: RecurringTransactionTemplate,
        currentDate: LocalDate,
    ): RecurringTransactionExecutionResult? {
        val scheduledFor = template.nextRunDate ?: return null

        if (template.status != RecurringTransactionTemplateStatus.ACTIVE || scheduledFor > currentDate) {
            return null
        }

        accountValidator.validateActiveAccount(template.accountId, template.userId)
        categoryValidator.validateActiveCategory(
            categoryId = template.categoryId,
            userId = template.userId,
            transactionType = template.type,
        )

        transactionService.saveScheduled(template, scheduledFor)

        val candidateNextRunDate = scheduleProvider.nextDate(
            lastRunDate = scheduledFor,
            startDate = template.startDate,
            frequency = template.frequency,
            intervalCount = template.intervalCount,
        )
        val nextRunDate = candidateNextRunDate.takeUnless { template.endDate != null && it > template.endDate }

        executionStateService.recordExecution(
            template = template,
            lastRunDate = scheduledFor,
            nextRunDate = nextRunDate,
        )

        return RecurringTransactionExecutionResult(template.id, scheduledFor)
    }
}
