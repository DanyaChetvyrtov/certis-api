package ru.digitalhustle.certis.features.transaction.command.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.category.api.CategoryCommandAccess
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import ru.digitalhustle.certis.features.transaction.command.model.AssignTransactionsCategory
import ru.digitalhustle.certis.features.transaction.command.model.NewTransaction
import ru.digitalhustle.certis.features.transaction.command.model.UpdateTransactionData
import ru.digitalhustle.certis.features.transaction.command.service.TransactionCommandService
import ru.digitalhustle.certis.features.transaction.command.service.TransactionService
import ru.digitalhustle.certis.features.transaction.command.validator.AccountValidator
import ru.digitalhustle.certis.features.transaction.command.validator.CategoryValidator
import ru.digitalhustle.certis.features.transaction.command.validator.TransactionValidator
import ru.digitalhustle.certis.features.transaction.model.Transaction
import java.util.UUID

@Service
class TransactionCommandServiceImpl(
    private val transactionService: TransactionService,
    private val categoryService: CategoryCommandAccess,
    private val accountValidator: AccountValidator,
    private val categoryValidator: CategoryValidator,
    private val transactionValidator: TransactionValidator,
) : TransactionCommandService {

    @Transactional
    override fun save(transaction: NewTransaction): Transaction {
        accountValidator.validateActiveAccount(transaction.accountId, transaction.userId)
        categoryValidator.validateCategory(transaction.categoryId, transaction.userId)
            ?.let { category -> transactionValidator.validateCategoryType(category, transaction.type) }

        return transactionService.save(transaction)
    }

    @Transactional
    override fun update(transaction: UpdateTransactionData): Transaction {
        val currentTransaction = transactionService.getByIdForUpdate(transaction.id, transaction.userId)
        transactionValidator.validateNotTransferPosting(currentTransaction)

        if (currentTransaction.accountId != transaction.accountId) {
            accountValidator.validateActiveAccount(transaction.accountId, transaction.userId)
        }
        categoryValidator.validateCategory(
            categoryId = transaction.categoryId,
            userId = transaction.userId,
            allowArchived = transaction.categoryId == currentTransaction.categoryId,
        )?.let { category -> transactionValidator.validateCategoryType(category, transaction.type) }

        return transactionService.update(transaction)
    }

    @Transactional
    override fun assignCategories(
        assignment: AssignTransactionsCategory,
    ) {
        transactionValidator.validateUniqueAssignments(assignment)

        val categories = categoryService.getAllByIdsForShare(
            assignment.assignments.map { it.categoryId }.toSet(),
            assignment.userId,
        ).associateBy(CategorySnapshot::id)
        categories.values.forEach { category ->
            categoryValidator.validateNotArchived(category)
        }

        val transactions = transactionService.getAllByIdsForUpdate(
            assignment.assignments.map { it.transactionId },
            assignment.userId,
        ).associateBy(Transaction::id)

        assignment.assignments.forEach { item ->
            val transaction = transactions.getValue(item.transactionId)
            val category = categories.getValue(item.categoryId)
            transactionValidator.validateNotTransferPosting(transaction)
            transactionValidator.validateUncategorized(transaction)
            transactionValidator.validateCategoryType(category, transaction.type)
        }

        transactionService.assignCategories(assignment)
    }

    @Transactional
    override fun delete(
        id: UUID,
        userId: UUID,
    ) {
        transactionService.findByIdForUpdate(id, userId)?.let(transactionValidator::validateNotTransferPosting)
        transactionService.delete(id, userId)
    }
}
