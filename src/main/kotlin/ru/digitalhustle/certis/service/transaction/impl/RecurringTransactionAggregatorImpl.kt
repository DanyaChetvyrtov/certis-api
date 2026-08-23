package ru.digitalhustle.certis.service.transaction.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.exception.custom.AccountClosedException
import ru.digitalhustle.certis.exception.custom.CategoryArchivedException
import ru.digitalhustle.certis.exception.custom.InvalidRecurringTransactionException
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.model.entity.RecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.NewRecurringTransactionTemplate
import ru.digitalhustle.certis.model.transaction.UpdateRecurringTransactionTemplateData
import ru.digitalhustle.certis.provider.RecurringTransactionScheduleProvider
import ru.digitalhustle.certis.service.domain.AccountService
import ru.digitalhustle.certis.service.domain.CategoryService
import ru.digitalhustle.certis.service.domain.RecurringTransactionTemplateService
import ru.digitalhustle.certis.service.transaction.RecurringTransactionAggregator
import java.time.LocalDate
import java.util.UUID

@Service
class RecurringTransactionAggregatorImpl(
    private val recurringTransactionTemplateService: RecurringTransactionTemplateService,
    private val accountService: AccountService,
    private val categoryService: CategoryService,
    private val scheduleProvider: RecurringTransactionScheduleProvider,
) : RecurringTransactionAggregator {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): RecurringTransactionTemplate = recurringTransactionTemplateService.getById(id, userId)

    override fun getAllByUserId(userId: UUID): List<RecurringTransactionTemplate> =
        recurringTransactionTemplateService.getAllByUserId(userId)

    @Transactional
    override fun save(template: NewRecurringTransactionTemplate): RecurringTransactionTemplate {
        validateDateRange(template.startDate, template.endDate)
        validateActiveAccount(template.accountId, template.userId)
        validateActiveCategory(template.categoryId, template.type, template.userId)

        return recurringTransactionTemplateService.save(template)
    }

    @Transactional
    override fun update(template: UpdateRecurringTransactionTemplateData): RecurringTransactionTemplate {
        val currentTemplate = recurringTransactionTemplateService.getByIdForUpdate(template.id, template.userId)

        validateMutable(currentTemplate)
        validateSchedulableStatus(template.status)
        validateDateRange(template.startDate, template.endDate, currentTemplate.lastRunDate)
        validateActiveAccount(template.accountId, template.userId)
        validateActiveCategory(template.categoryId, template.type, template.userId)

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

        if (template.status in TERMINAL_STATUSES) {
            return
        }
        recurringTransactionTemplateService.cancel(template)
    }

    private fun validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate?,
        lastRunDate: LocalDate? = null,
    ) {
        if (endDate != null && (endDate < startDate || lastRunDate != null && endDate < lastRunDate)) {
            throw InvalidRecurringTransactionException(ErrorMessages.RECURRING_TRANSACTION_DATE_RANGE)
        }
        if (lastRunDate != null && startDate > lastRunDate) {
            throw InvalidRecurringTransactionException(ErrorMessages.RECURRING_TRANSACTION_DATE_RANGE)
        }
    }

    private fun validateMutable(template: RecurringTransactionTemplate) {
        if (template.status in TERMINAL_STATUSES) {
            throw InvalidRecurringTransactionException(ErrorMessages.RECURRING_TRANSACTION_TERMINAL)
        }
    }

    private fun validateSchedulableStatus(status: RecurringTransactionTemplateStatus) {
        if (status !in SCHEDULABLE_STATUSES) {
            throw InvalidRecurringTransactionException(ErrorMessages.RECURRING_TRANSACTION_STATUS)
        }
    }

    private fun validateActiveAccount(
        accountId: UUID,
        userId: UUID,
    ) {
        val account = accountService.getByIdForShare(accountId, userId)

        if (account.closedAt != null) {
            throw AccountClosedException(ErrorMessages.TRANSACTION_ACCOUNT_CLOSED)
        }
    }

    private fun validateActiveCategory(
        categoryId: UUID?,
        transactionType: TransactionType,
        userId: UUID,
    ) {
        if (categoryId == null) {
            return
        }

        val category = categoryService.getByIdForShare(categoryId, userId)

        validateCategoryArchive(category)
        validateCategoryType(category, transactionType)
    }

    private fun validateCategoryArchive(category: Category) {
        if (category.archivedAt != null) {
            throw CategoryArchivedException(ErrorMessages.TRANSACTION_CATEGORY_ARCHIVED)
        }
    }

    private fun validateCategoryType(
        category: Category,
        transactionType: TransactionType,
    ) {
        if (category.type != CategoryType.valueOf(transactionType.name)) {
            throw InvalidRecurringTransactionException(ErrorMessages.TRANSACTION_CATEGORY_TYPE_MISMATCH)
        }
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

    private companion object {
        private val TERMINAL_STATUSES = setOf(
            RecurringTransactionTemplateStatus.COMPLETED,
            RecurringTransactionTemplateStatus.CANCELLED,
        )
        private val SCHEDULABLE_STATUSES = setOf(
            RecurringTransactionTemplateStatus.ACTIVE,
            RecurringTransactionTemplateStatus.PAUSED,
        )
    }
}
