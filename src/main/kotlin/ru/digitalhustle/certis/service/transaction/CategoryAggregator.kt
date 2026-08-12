package ru.digitalhustle.certis.service.transaction

import ru.digitalhustle.certis.model.CategoryPreview
import ru.digitalhustle.certis.model.NewCategory
import ru.digitalhustle.certis.model.UpdateCategoryData
import java.util.UUID

interface CategoryAggregator {

    fun getById(id: UUID, userId: UUID): CategoryPreview

    fun getAllByUserId(userId: UUID): List<CategoryPreview>

    fun save(category: NewCategory): CategoryPreview

    fun update(category: UpdateCategoryData): CategoryPreview

    fun restore(id: UUID, userId: UUID)

    fun archive(id: UUID, userId: UUID)
}
