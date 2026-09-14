package ru.digitalhustle.certis.features.transaction.command.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.category.api.CategoryCommandAccess
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import ru.digitalhustle.certis.features.category.exceptions.CategoryArchivedException
import ru.digitalhustle.certis.features.transaction.constants.TransactionErrorMessages
import java.util.UUID

@Component
class CategoryValidator(
    private val categoryService: CategoryCommandAccess,
) {

    fun validateCategory(
        categoryId: UUID?,
        userId: UUID,
        allowArchived: Boolean = false,
    ): CategorySnapshot? {
        val categoryId = categoryId ?: return null
        val category = categoryService.getByIdForShare(categoryId, userId)

        validateNotArchived(category, allowArchived)

        return category
    }

    fun validateNotArchived(
        category: CategorySnapshot,
        allowArchived: Boolean = false,
    ) {
        if (category.archivedAt != null && !allowArchived) {
            throw CategoryArchivedException(TransactionErrorMessages.TRANSACTION_CATEGORY_ARCHIVED)
        }
    }
}
