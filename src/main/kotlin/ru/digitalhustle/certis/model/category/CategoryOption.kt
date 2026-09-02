package ru.digitalhustle.certis.model.category

import java.util.UUID

data class CategoryOption(

    val id: UUID,

    val name: String,

    val icon: String,

    val color: String,
)
