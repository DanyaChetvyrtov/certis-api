package ru.digitalhustle.certis.features.transaction.command.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.transaction.command.model.AssignTransactionsCategory
import ru.digitalhustle.certis.features.transaction.constants.TransactionErrorMessages
import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import ru.digitalhustle.certis.features.transaction.exceptions.InvalidTransactionException
import ru.digitalhustle.certis.features.transaction.model.Transaction

@Component
class TransactionValidator {

    fun validateCategoryType(
        category: CategorySnapshot,
        transactionType: TransactionType,
    ) {
        if (category.type != CategoryType.valueOf(transactionType.name)) {
            throw InvalidTransactionException(TransactionErrorMessages.TRANSACTION_CATEGORY_TYPE_MISMATCH)
        }
    }

    fun validateNotTransferPosting(transaction: Transaction) {
        if (transaction.transferId != null) {
            throw InvalidTransactionException(TransactionErrorMessages.TRANSFER_TRANSACTION_IMMUTABLE)
        }
    }

    fun validateUncategorized(transaction: Transaction) {
        if (transaction.categoryId != null) {
            throw InvalidTransactionException(TransactionErrorMessages.TRANSACTION_ALREADY_CATEGORIZED)
        }
    }

    fun validateUniqueAssignments(assignment: AssignTransactionsCategory) {
        val transactionIds = assignment.assignments.map { item -> item.transactionId }

        if (transactionIds.toSet().size != transactionIds.size) {
            throw InvalidTransactionException(TransactionErrorMessages.TRANSACTION_DUPLICATE_CATEGORY_ASSIGNMENTS)
        }
    }
}
