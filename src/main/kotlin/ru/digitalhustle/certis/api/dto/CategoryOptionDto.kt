package ru.digitalhustle.certis.api.dto

import java.util.UUID

data class CategoryOptionDto(

    val id: UUID,

    val name: String,

    val icon: String,

    val color: String,
)
