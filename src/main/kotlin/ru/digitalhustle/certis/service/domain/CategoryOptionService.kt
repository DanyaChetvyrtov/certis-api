package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.model.category.CategoryOption
import java.util.UUID

interface CategoryOptionService {

    fun getOptions(userId: UUID, type: CategoryType): List<CategoryOption>
}
