package ru.digitalhustle.certis.exception.custom

abstract class DomainException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
