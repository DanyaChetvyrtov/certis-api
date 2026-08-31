package ru.digitalhustle.certis.exception.custom

class InvalidBudgetException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause)
