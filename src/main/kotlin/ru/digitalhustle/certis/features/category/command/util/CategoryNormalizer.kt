package ru.digitalhustle.certis.features.category.command.util

import ru.digitalhustle.certis.features.category.command.model.NewCategory
import ru.digitalhustle.certis.features.category.command.model.UpdateCategoryData

object CategoryNormalizer {

    fun normalize(category: NewCategory): NewCategory =
        category.copy(
            name = category.name.trim(),
            icon = category.icon.trim(),
        )

    fun normalize(category: UpdateCategoryData): UpdateCategoryData =
        category.copy(
            name = category.name.trim(),
            icon = category.icon.trim(),
        )
}
