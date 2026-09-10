package ru.digitalhustle.certis.features.transaction.command.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.transaction.constants.TransactionErrorMessages
import ru.digitalhustle.certis.features.transaction.enums.RecurringTransactionTemplateStatus
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.exceptions.InvalidRecurringTransactionException
import ru.digitalhustle.certis.features.transaction.model.RecurringTransactionTemplate
import java.time.LocalDate

@Component
class RecurringTransactionValidator {

    fun validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate?,
        lastRunDate: LocalDate? = null,
    ) {
        if (endDate != null && (endDate < startDate || lastRunDate != null && endDate < lastRunDate)) {
            throw InvalidRecurringTransactionException(TransactionErrorMessages.RECURRING_TRANSACTION_DATE_RANGE)
        }
        if (lastRunDate != null && startDate > lastRunDate) {
            throw InvalidRecurringTransactionException(TransactionErrorMessages.RECURRING_TRANSACTION_DATE_RANGE)
        }
    }

    fun validateMutable(template: RecurringTransactionTemplate) {
        if (isTerminal(template.status)) {
            throw InvalidRecurringTransactionException(TransactionErrorMessages.RECURRING_TRANSACTION_TERMINAL)
        }
    }

    fun isTerminal(status: RecurringTransactionTemplateStatus): Boolean = status in TERMINAL_STATUSES

    fun validateSchedulableStatus(status: RecurringTransactionTemplateStatus) {
        if (status !in SCHEDULABLE_STATUSES) {
            throw InvalidRecurringTransactionException(TransactionErrorMessages.RECURRING_TRANSACTION_STATUS)
        }
    }

    fun validateCategoryType(
        category: CategorySnapshot,
        transactionType: TransactionType,
    ) {
        if (category.type != CategoryType.valueOf(transactionType.name)) {
            throw InvalidRecurringTransactionException(TransactionErrorMessages.TRANSACTION_CATEGORY_TYPE_MISMATCH)
        }
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
