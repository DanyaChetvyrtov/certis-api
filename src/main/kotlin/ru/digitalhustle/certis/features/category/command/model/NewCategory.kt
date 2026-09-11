package ru.digitalhustle.certis.features.category.command.model

import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.util.UUID

data class NewCategory(

    val userId: UUID,

    val name: String,

    val type: CategoryType,

    val icon: String,

    val color: String,
)
