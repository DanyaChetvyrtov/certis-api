package ru.digitalhustle.certis.service.transaction.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.exception.custom.CategoryInUseException
import ru.digitalhustle.certis.model.CategoryPreview
import ru.digitalhustle.certis.model.NewCategory
import ru.digitalhustle.certis.model.UpdateCategoryData
import ru.digitalhustle.certis.model.category.CategoryAnalytics
import ru.digitalhustle.certis.model.category.CategoryAnalyticsFilter
import ru.digitalhustle.certis.model.category.CategoryCardFilter
import ru.digitalhustle.certis.model.category.CategoryCards
import ru.digitalhustle.certis.model.category.CategoryOption
import ru.digitalhustle.certis.service.domain.CategoryAnalyticsService
import ru.digitalhustle.certis.service.domain.CategoryCardService
import ru.digitalhustle.certis.service.domain.CategoryOptionService
import ru.digitalhustle.certis.service.domain.CategoryService
import ru.digitalhustle.certis.service.transaction.CategoryAggregator
import ru.digitalhustle.certis.util.validation.CategoryValidator
import java.util.UUID

@Service
class CategoryAggregatorImpl(
    private val categoryService: CategoryService,
    private val categoryCardService: CategoryCardService,
    private val categoryAnalyticsService: CategoryAnalyticsService,
    private val categoryOptionService: CategoryOptionService,
    private val categoryValidator: CategoryValidator,
) : CategoryAggregator {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): CategoryPreview = categoryService.getById(id, userId)

    override fun getCards(
        userId: UUID,
        filter: CategoryCardFilter,
    ): CategoryCards = categoryCardService.getCards(userId, filter)

    override fun getAnalytics(
        userId: UUID,
        filter: CategoryAnalyticsFilter,
    ): CategoryAnalytics = categoryAnalyticsService.getAnalytics(userId, filter)

    override fun getOptions(
        userId: UUID,
        type: CategoryType,
    ): List<CategoryOption> = categoryOptionService.getOptions(userId, type)

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
        if (categoryValidator.isRequired(id, userId)) {
            throw CategoryInUseException(ErrorMessages.CATEGORY_IN_USE)
        }

        categoryService.archive(id, userId)
    }
}
