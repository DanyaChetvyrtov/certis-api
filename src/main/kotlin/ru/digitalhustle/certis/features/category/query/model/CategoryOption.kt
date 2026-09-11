package ru.digitalhustle.certis.features.category.query.model

import java.util.UUID

data class CategoryOption(

    val id: UUID,

    val name: String,

    val icon: String,

    val color: String,
)
