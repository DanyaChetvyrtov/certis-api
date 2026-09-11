package ru.digitalhustle.certis.features.category.application.service

import ru.digitalhustle.certis.features.category.command.model.NewCategory
import ru.digitalhustle.certis.features.category.command.model.UpdateCategoryData
import ru.digitalhustle.certis.features.category.model.CategoryPreview
import java.util.UUID

interface CategoryApplicationService {

    fun save(category: NewCategory): CategoryPreview

    fun update(category: UpdateCategoryData): CategoryPreview

    fun restore(id: UUID, userId: UUID)

    fun archive(id: UUID, userId: UUID)
}
