package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.CategoryType
import java.time.OffsetDateTime
import java.util.UUID

data class Category(

    val id: UUID,

    val userId: UUID,

    val name: String,

    val type: CategoryType,

    val icon: String,

    val color: String,

    val archivedAt: OffsetDateTime?,
)
