package ru.digitalhustle.certis.model.security

data class UserCredentials(

    val email: String,

    val password: String,

    val passwordConfirmation: String?,
)
