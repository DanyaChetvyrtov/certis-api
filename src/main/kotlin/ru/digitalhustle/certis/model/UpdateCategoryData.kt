package ru.digitalhustle.certis.model

import java.util.UUID

data class UpdateCategoryData(

    val id: UUID,

    val userId: UUID,

    val name: String,

    val icon: String,

    val color: String,
)
