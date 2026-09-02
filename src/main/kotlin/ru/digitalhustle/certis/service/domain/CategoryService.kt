package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.CategoryPreview
import ru.digitalhustle.certis.model.NewCategory
import ru.digitalhustle.certis.model.UpdateCategoryData
import ru.digitalhustle.certis.model.entity.Category
import java.util.UUID

interface CategoryService {

    fun getById(id: UUID, userId: UUID): CategoryPreview

    fun getByIdForShare(id: UUID, userId: UUID): Category

    fun getAllByIdsForShare(ids: Collection<UUID>, userId: UUID): List<Category>

    fun getByIdForUpdate(id: UUID, userId: UUID): Category

    fun getAllByUserId(userId: UUID): List<CategoryPreview>

    fun save(category: NewCategory): CategoryPreview

    fun createDefaults(userId: UUID)

    fun update(category: UpdateCategoryData): CategoryPreview

    fun restore(id: UUID, userId: UUID)

    fun archive(id: UUID, userId: UUID)
}
