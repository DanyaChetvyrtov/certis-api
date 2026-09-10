package ru.digitalhustle.certis.features.category.query.service

import ru.digitalhustle.certis.features.category.model.CategoryPreview
import java.util.UUID

interface CategoryQueryService {

    fun getById(id: UUID, userId: UUID): CategoryPreview

    fun getAllByUserId(userId: UUID): List<CategoryPreview>
}
