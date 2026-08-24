package ru.digitalhustle.certis.exception.custom

abstract class SystemException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
