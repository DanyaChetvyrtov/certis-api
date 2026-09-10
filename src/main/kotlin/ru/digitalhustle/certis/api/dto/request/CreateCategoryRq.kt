package ru.digitalhustle.certis.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import ru.digitalhustle.certis.features.category.enums.CategoryType

data class CreateCategoryRq(

    @field:NotBlank
    @field:Size(max = 150, message = "should be less than {max}")
    val name: String,

    val type: CategoryType,

    @field:NotBlank
    @field:Size(max = 50, message = "should be less than {max}")
    val icon: String,

    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    val color: String,
)
