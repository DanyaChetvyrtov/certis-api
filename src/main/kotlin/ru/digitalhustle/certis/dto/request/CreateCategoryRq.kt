package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import ru.digitalhustle.certis.enums.CategoryType

data class CreateCategoryRq(

    @field:NotBlank
    @field:Size(max = 150, message = "should be less than {max}")
    val name: String,

    val type: CategoryType,

    @field:NotBlank
    val icon: String,

    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    val color: String,
)
