package ru.digitalhustle.certis.util.validation

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.TransactionType
import ru.digitalhustle.certis.exception.custom.CategoryArchivedException
import ru.digitalhustle.certis.exception.custom.InvalidRecurringTransactionException
import ru.digitalhustle.certis.model.entity.Category
import ru.digitalhustle.certis.repository.CategoryUsageRepository
import ru.digitalhustle.certis.service.domain.CategoryService
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Component
class CategoryValidator(
    private val clock: Clock,
    private val categoryService: CategoryService,
    private val categoryUsageRepository: CategoryUsageRepository,
) {

    fun validateActiveCategory(
        categoryId: UUID?,
        userId: UUID,
        transactionType: TransactionType,
    ) {
        val categoryId = categoryId ?: return
        val category = categoryService.getByIdForShare(categoryId, userId)

        validateCategoryArchive(category)
        validateCategoryType(category, transactionType)
    }

    fun isRequired(
        categoryId: UUID,
        userId: UUID,
    ): Boolean =
        categoryUsageRepository.existsInSchedulableRecurringTemplate(categoryId, userId) ||
            categoryUsageRepository.existsInCurrentOrFutureBudget(
                categoryId = categoryId,
                userId = userId,
                currentMonth = LocalDate.now(clock).withDayOfMonth(1),
            )

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
}
