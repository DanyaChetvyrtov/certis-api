package ru.digitalhustle.certis.service.transaction

import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.model.CategoryPreview
import ru.digitalhustle.certis.model.NewCategory
import ru.digitalhustle.certis.model.UpdateCategoryData
import ru.digitalhustle.certis.model.category.CategoryAnalytics
import ru.digitalhustle.certis.model.category.CategoryAnalyticsFilter
import ru.digitalhustle.certis.model.category.CategoryCardFilter
import ru.digitalhustle.certis.model.category.CategoryCards
import ru.digitalhustle.certis.model.category.CategoryOption
import java.util.UUID

interface CategoryAggregator {

    fun getById(id: UUID, userId: UUID): CategoryPreview

    fun getCards(userId: UUID, filter: CategoryCardFilter): CategoryCards

    fun getAnalytics(userId: UUID, filter: CategoryAnalyticsFilter): CategoryAnalytics

    fun getOptions(userId: UUID, type: CategoryType): List<CategoryOption>

    fun save(category: NewCategory): CategoryPreview

    fun update(category: UpdateCategoryData): CategoryPreview

    fun restore(id: UUID, userId: UUID)

    fun archive(id: UUID, userId: UUID)
}
