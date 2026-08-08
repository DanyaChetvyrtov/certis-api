package ru.digitalhustle.certis.exception.custom

class AccountClosedException(
    message: String,
) : DomainException(message)
