package ru.digitalhustle.certis.features.category.api

import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.time.OffsetDateTime
import java.util.UUID

data class CategorySnapshot(

    val id: UUID,

    val type: CategoryType,

    val name: String = "",

    val icon: String = "",

    val color: String = "",

    val archivedAt: OffsetDateTime?,
)
