package ru.digitalhustle.certis.features.category.model

fun Category.toPreview(): CategoryPreview =
    CategoryPreview(
        id = id,
        name = name,
        type = type,
        icon = icon,
        color = color,
        archivedAt = archivedAt,
    )
