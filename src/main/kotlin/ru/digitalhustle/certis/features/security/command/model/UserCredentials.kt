package ru.digitalhustle.certis.features.security.command.model

data class UserCredentials(

    val email: String,

    val password: String,

    val passwordConfirmation: String?,
)
