package ru.digitalhustle.certis.features.category.query.service

import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.category.query.model.CategoryOption
import java.util.UUID

interface CategoryOptionService {

    fun getOptions(userId: UUID, type: CategoryType): List<CategoryOption>
}
