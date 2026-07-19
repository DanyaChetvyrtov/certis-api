package ru.digitalhustle.certis.features.transaction.command.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.transaction.command.model.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.features.transaction.command.model.UpdateRecurringTransactionTemplateData
import ru.digitalhustle.certis.features.transaction.command.service.RecurringTransactionCommandService
import ru.digitalhustle.certis.features.transaction.command.service.RecurringTransactionTemplateService
import ru.digitalhustle.certis.features.transaction.command.validator.AccountValidator
import ru.digitalhustle.certis.features.transaction.command.validator.CategoryValidator
import ru.digitalhustle.certis.features.transaction.command.validator.RecurringTransactionValidator
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import ru.digitalhustle.certis.scheduler.RecurringTransactionScheduleProvider
import java.time.LocalDate
import java.util.UUID

@Service
class RecurringTransactionCommandServiceImpl(
    private val recurringTransactionTemplateService: RecurringTransactionTemplateService,
    private val scheduleProvider: RecurringTransactionScheduleProvider,
    private val accountValidator: AccountValidator,
    private val categoryValidator: CategoryValidator,
    private val recurringTransactionValidator: RecurringTransactionValidator,
) : RecurringTransactionCommandService {

    @Transactional
    override fun save(template: NewRecurringTransactionTemplate): RecurringTransactionTemplate {
        recurringTransactionValidator.validateDateRange(template.startDate, template.endDate)
        accountValidator.validateActiveAccount(template.accountId, template.userId)
        categoryValidator.validateCategory(template.categoryId, template.userId)
            ?.let { category -> recurringTransactionValidator.validateCategoryType(category, template.type) }

        return recurringTransactionTemplateService.save(template)
    }

    @Transactional
    override fun update(template: UpdateRecurringTransactionTemplateData): RecurringTransactionTemplate {
        val currentTemplate = recurringTransactionTemplateService.getByIdForUpdate(template.id, template.userId)

        recurringTransactionValidator.validateMutable(currentTemplate)
        recurringTransactionValidator.validateSchedulableStatus(template.status)
        recurringTransactionValidator.validateDateRange(
            template.startDate,
            template.endDate,
            currentTemplate.lastRunDate,
        )
        accountValidator.validateActiveAccount(template.accountId, template.userId)
        categoryValidator.validateCategory(template.categoryId, template.userId)
            ?.let { category -> recurringTransactionValidator.validateCategoryType(category, template.type) }

        return recurringTransactionTemplateService.update(
            currentTemplate = currentTemplate,
            updateData = template,
            nextRunDate = calculateNextRunDate(currentTemplate, template),
        )
    }

    @Transactional
    override fun cancel(
        id: UUID,
        userId: UUID,
    ) {
        val template = recurringTransactionTemplateService.getByIdForUpdate(id, userId)

        if (recurringTransactionValidator.isTerminal(template.status)) {
            return
        }
        recurringTransactionTemplateService.cancel(template)
    }

    private fun calculateNextRunDate(
        currentTemplate: RecurringTransactionTemplate,
        updateData: UpdateRecurringTransactionTemplateData,
    ): LocalDate? {
        val nextRunDate = currentTemplate.lastRunDate?.let { lastRunDate ->
            scheduleProvider.nextDate(
                lastRunDate = lastRunDate,
                startDate = updateData.startDate,
                frequency = updateData.frequency,
                intervalCount = updateData.intervalCount,
            )
        } ?: updateData.startDate

        return nextRunDate.takeUnless { updateData.endDate != null && it > updateData.endDate }
    }
}
