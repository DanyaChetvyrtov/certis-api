package ru.digitalhustle.certis.util.normalizer

import ru.digitalhustle.certis.model.NewCategory
import ru.digitalhustle.certis.model.UpdateCategoryData

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
