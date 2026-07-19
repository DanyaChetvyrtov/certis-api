package ru.digitalhustle.certis.exception.custom

class AccountInUseException(
    message: String,
) : DomainException(message)
