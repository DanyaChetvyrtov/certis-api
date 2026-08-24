package ru.digitalhustle.certis.exception.custom

class PhotoProcessingException(
    message: String,
    cause: Throwable? = null,
) : SystemException(message, cause)
