package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import ru.digitalhustle.certis.enums.Currency

data class CreateUserRq(

    @Email
    @NotBlank
    val email: String,

    @NotBlank
    val password: String,

    val preferredCurrency: Currency? = null,
)
