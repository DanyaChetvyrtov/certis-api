package ru.digitalhustle.certis.model

import ru.digitalhustle.certis.enums.CategoryType
import java.util.UUID

data class NewCategory(

    val userId: UUID,

    val name: String,

    val type: CategoryType,

    val icon: String,

    val color: String,
)
