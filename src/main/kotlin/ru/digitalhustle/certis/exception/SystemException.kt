package ru.digitalhustle.certis.exception

abstract class SystemException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)