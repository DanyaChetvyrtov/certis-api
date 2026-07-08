package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRq(

    @Email
    @NotBlank
    val email: String,

    @NotBlank
    val password: String,
)
