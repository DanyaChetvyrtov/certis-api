package ru.digitalhustle.certis.dto.response

import java.util.UUID

data class CategoryOptionRs(

    val id: UUID,

    val name: String,

    val icon: String,

    val color: String,
)
