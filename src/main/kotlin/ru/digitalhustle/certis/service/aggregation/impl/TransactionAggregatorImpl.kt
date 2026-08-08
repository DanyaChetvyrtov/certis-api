package ru.digitalhustle.certis.service.aggregation.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.exception.custom.AccountClosedException
import ru.digitalhustle.certis.exception.custom.CategoryArchivedException
import ru.digitalhustle.certis.exception.custom.InvalidTransactionException
import ru.digitalhustle.certis.model.NewTransaction
import ru.digitalhustle.certis.model.TransactionFilter
import ru.digitalhustle.certis.model.TransactionPage
import ru.digitalhustle.certis.model.UpdateTransactionData
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.model.entity.Transaction
import ru.digitalhustle.certis.service.aggregation.TransactionAggregator
import ru.digitalhustle.certis.service.domain.AccountService
import ru.digitalhustle.certis.service.domain.CategoryService
import ru.digitalhustle.certis.service.domain.TransactionService
import java.util.UUID

@Service
class TransactionAggregatorImpl(
    private val transactionService: TransactionService,
    private val accountService: AccountService,
    private val categoryService: CategoryService,
) : TransactionAggregator {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): Transaction = transactionService.getById(id, userId)

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getAllByUserId(
        userId: UUID,
        filter: TransactionFilter,
    ): TransactionPage = transactionService.getAllByUserId(userId, filter)

    @Transactional
    override fun save(transaction: NewTransaction): Transaction {
        validateActiveAccount(transaction.accountId, transaction.userId)
        validateCategory(transaction.categoryId, transaction.type, transaction.userId)

        return transactionService.save(transaction)
    }

    @Transactional
    override fun update(transaction: UpdateTransactionData): Transaction {
        val currentTransaction = transactionService.getByIdForUpdate(transaction.id, transaction.userId)

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
    override fun delete(
        id: UUID,
        userId: UUID,
    ): Unit = transactionService.delete(id, userId)

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
}
