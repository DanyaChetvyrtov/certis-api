package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateCategoryRq(

    @field:NotBlank
    @field:Size(max = 150, message = "should be less than {max}")
    val name: String,

    @field:NotBlank
    @field:Size(max = 50, message = "should be less than {max}")
    val icon: String,

    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    val color: String,
)
