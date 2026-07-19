package ru.digitalhustle.certis.dto

import com.fasterxml.jackson.annotation.JsonInclude
import ru.digitalhustle.certis.enums.CategoryType
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CategoryDto(

    val id: UUID,

    val name: String,

    val type: CategoryType,

    val icon: String,

    val color: String,

    val archivedAt: OffsetDateTime?,
)
