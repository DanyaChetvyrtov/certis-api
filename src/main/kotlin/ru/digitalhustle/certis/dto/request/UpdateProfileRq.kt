package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UpdateProfileRq(

    @field:NotBlank
    @field:Size(max = 100, message = "should be less than {max}")
    val name: String,

    @field:NotBlank
    @field:Size(max = 100, message = "should be less than {max}")
    val surname: String,

    @field:Past
    val dateOfBirth: LocalDate,
)
