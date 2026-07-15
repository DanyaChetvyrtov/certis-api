package ru.digitalhustle.certis.model

data class UserCredentials(

    val email: String,

    val password: String,

    val passwordConfirmation: String?
)
