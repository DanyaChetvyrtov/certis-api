package ru.digitalhustle.certis.features.profile.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class InvalidPhotoException(
    message: String,
) : DomainException(message)
