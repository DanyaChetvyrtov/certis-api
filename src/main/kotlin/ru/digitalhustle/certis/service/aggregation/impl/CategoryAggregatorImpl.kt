package ru.digitalhustle.certis.service.aggregation.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.CategoryInUseException
import ru.digitalhustle.certis.model.CategoryPreview
import ru.digitalhustle.certis.model.NewCategory
import ru.digitalhustle.certis.model.UpdateCategoryData
import ru.digitalhustle.certis.provider.CategoryUsageProvider
import ru.digitalhustle.certis.service.aggregation.CategoryAggregator
import ru.digitalhustle.certis.service.domain.CategoryService
import java.util.UUID

@Service
class CategoryAggregatorImpl(
    private val categoryService: CategoryService,
    private val categoryUsageProvider: CategoryUsageProvider,
) : CategoryAggregator {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): CategoryPreview = categoryService.getById(id, userId)

    override fun getAllByUserId(userId: UUID): List<CategoryPreview> =
        categoryService.getAllByUserId(userId)

    override fun save(category: NewCategory): CategoryPreview = categoryService.save(category)

    override fun update(category: UpdateCategoryData): CategoryPreview = categoryService.update(category)

    override fun restore(
        id: UUID,
        userId: UUID,
    ): Unit = categoryService.restore(id, userId)

    @Transactional
    override fun archive(
        id: UUID,
        userId: UUID,
    ) {
        val category = categoryService.getByIdForUpdate(id, userId)

        if (category.archivedAt != null) {
            return
        }
        if (categoryUsageProvider.isRequired(id, userId)) {
            throw CategoryInUseException(ErrorMessages.CATEGORY_IN_USE)
        }

        categoryService.archive(id, userId)
    }
}
