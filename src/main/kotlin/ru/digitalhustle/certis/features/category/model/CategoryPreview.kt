package ru.digitalhustle.certis.features.category.model

import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.time.OffsetDateTime
import java.util.UUID

data class CategoryPreview(

    val id: UUID,

    val name: String,

    val type: CategoryType,

    val icon: String,

    val color: String,

    val archivedAt: OffsetDateTime?,
)
