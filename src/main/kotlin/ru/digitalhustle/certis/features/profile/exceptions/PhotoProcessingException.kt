package ru.digitalhustle.certis.features.profile.exceptions

import ru.digitalhustle.certis.exception.custom.SystemException

class PhotoProcessingException(
    message: String,
    cause: Throwable? = null,
) : SystemException(message, cause)
