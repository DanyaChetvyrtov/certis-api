package ru.digitalhustle.certis.service.transaction.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.exception.custom.AccountClosedException
import ru.digitalhustle.certis.exception.custom.CategoryArchivedException
import ru.digitalhustle.certis.exception.custom.InvalidTransactionException
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.model.transaction.AssignTransactionsCategory
import ru.digitalhustle.certis.model.transaction.NewTransaction
import ru.digitalhustle.certis.model.transaction.TransactionFilter
import ru.digitalhustle.certis.model.transaction.TransactionPage
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionFilter
import ru.digitalhustle.certis.model.transaction.UncategorizedTransactionPage
import ru.digitalhustle.certis.model.transaction.UpdateTransactionData
import ru.digitalhustle.certis.service.domain.AccountService
import ru.digitalhustle.certis.service.domain.CategoryService
import ru.digitalhustle.certis.service.domain.TransactionService
import ru.digitalhustle.certis.service.domain.UncategorizedTransactionService
import ru.digitalhustle.certis.service.transaction.TransactionAggregator
import java.util.UUID

@Service
class TransactionAggregatorImpl(
    private val transactionService: TransactionService,
    private val accountService: AccountService,
    private val categoryService: CategoryService,
    private val uncategorizedTransactionService: UncategorizedTransactionService,
) : TransactionAggregator {

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    override fun getAllByUserId(
        userId: UUID,
        filter: TransactionFilter,
    ): TransactionPage = transactionService.getAllByUserId(userId, filter)

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getUncategorizedByUserId(
        userId: UUID,
        filter: UncategorizedTransactionFilter,
    ): UncategorizedTransactionPage =
        uncategorizedTransactionService.getAllByUserId(userId, filter)

    override fun getById(
        id: UUID,
        userId: UUID,
    ): Transaction = transactionService.getById(id, userId)

    @Transactional
    override fun save(transaction: NewTransaction): Transaction {
        validateActiveAccount(transaction.accountId, transaction.userId)
        validateCategory(transaction.categoryId, transaction.type, transaction.userId)

        return transactionService.save(transaction)
    }

    @Transactional
    override fun update(transaction: UpdateTransactionData): Transaction {
        val currentTransaction = transactionService.getByIdForUpdate(transaction.id, transaction.userId)
        validateNotTransferPosting(currentTransaction)

        if (currentTransaction.accountId != transaction.accountId) {
            validateActiveAccount(transaction.accountId, transaction.userId)
        }
        validateCategory(
            categoryId = transaction.categoryId,
            transactionType = transaction.type,
            userId = transaction.userId,
            allowArchived = transaction.categoryId == currentTransaction.categoryId,
        )

        return transactionService.update(transaction)
    }

    @Transactional
    override fun assignCategories(
        assignment: AssignTransactionsCategory,
    ) {
        validateUniqueTransactionAssignments(assignment)

        val categories = categoryService.getAllByIdsForShare(
            assignment.assignments.map { it.categoryId }.toSet(),
            assignment.userId,
        ).associateBy(Category::id)
        categories.values.forEach { category ->
            validateCategoryArchive(category, allowArchived = false)
        }

        val transactions = transactionService.getAllByIdsForUpdate(
            assignment.assignments.map { it.transactionId },
            assignment.userId,
        ).associateBy(Transaction::id)

        assignment.assignments.forEach { item ->
            val transaction = transactions.getValue(item.transactionId)
            val category = categories.getValue(item.categoryId)
            validateNotTransferPosting(transaction)
            validateUncategorized(transaction)
            validateCategoryType(category, transaction.type)
        }

        transactionService.assignCategories(assignment)
    }

    @Transactional
    override fun delete(
        id: UUID,
        userId: UUID,
    ) {
        transactionService.findByIdForUpdate(id, userId)?.let(::validateNotTransferPosting)
        transactionService.delete(id, userId)
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

    private fun validateCategory(
        categoryId: UUID?,
        transactionType: TransactionType,
        userId: UUID,
        allowArchived: Boolean = false,
    ) {
        if (categoryId == null) {
            return
        }

        val category = categoryService.getByIdForShare(categoryId, userId)

        validateCategoryArchive(category, allowArchived)
        validateCategoryType(category, transactionType)
    }

    private fun validateCategoryArchive(
        category: Category,
        allowArchived: Boolean,
    ) {
        if (category.archivedAt != null && !allowArchived) {
            throw CategoryArchivedException(ErrorMessages.TRANSACTION_CATEGORY_ARCHIVED)
        }
    }

    private fun validateCategoryType(
        category: Category,
        transactionType: TransactionType,
    ) {
        if (category.type != CategoryType.valueOf(transactionType.name)) {
            throw InvalidTransactionException(ErrorMessages.TRANSACTION_CATEGORY_TYPE_MISMATCH)
        }
    }

    private fun validateNotTransferPosting(transaction: Transaction) {
        if (transaction.transferId != null) {
            throw InvalidTransactionException(ErrorMessages.TRANSFER_TRANSACTION_IMMUTABLE)
        }
    }

    private fun validateUncategorized(transaction: Transaction) {
        if (transaction.categoryId != null) {
            throw InvalidTransactionException(ErrorMessages.TRANSACTION_ALREADY_CATEGORIZED)
        }
    }

    private fun validateUniqueTransactionAssignments(assignment: AssignTransactionsCategory) {
        val transactionIds = assignment.assignments.map { item -> item.transactionId }

        if (transactionIds.toSet().size != transactionIds.size) {
            throw InvalidTransactionException(ErrorMessages.TRANSACTION_DUPLICATE_CATEGORY_ASSIGNMENTS)
        }
    }
}
