package ru.digitalhustle.certis.mapper

import ru.digitalhustle.certis.model.CategoryPreview
import ru.digitalhustle.certis.model.entity.Category

fun Category.toPreview(): CategoryPreview =
    CategoryPreview(
        id = id,
        name = name,
        type = type,
        icon = icon,
        color = color,
        archivedAt = archivedAt,
    )
