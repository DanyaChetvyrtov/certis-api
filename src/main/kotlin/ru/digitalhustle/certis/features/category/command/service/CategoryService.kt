package ru.digitalhustle.certis.features.category.command.service

import ru.digitalhustle.certis.features.category.command.model.NewCategory
import ru.digitalhustle.certis.features.category.command.model.UpdateCategoryData
import ru.digitalhustle.certis.features.category.model.Category
import ru.digitalhustle.certis.features.category.model.CategoryPreview
import java.util.UUID

interface CategoryService {

    fun getByIdForShare(id: UUID, userId: UUID): Category

    fun getAllByIdsForShare(ids: Collection<UUID>, userId: UUID): List<Category>

    fun getByIdForUpdate(id: UUID, userId: UUID): Category

    fun save(category: NewCategory): CategoryPreview

    fun createDefaults(userId: UUID)

    fun update(category: UpdateCategoryData): CategoryPreview

    fun restore(id: UUID, userId: UUID)

    fun archive(id: UUID, userId: UUID)

    fun countActiveExpenseCategories(userId: UUID, categoryIds: Collection<UUID>): Int
}
