package ru.digitalhustle.certis.features.security.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class InvalidTokenException(
    message: String,
) : DomainException(message)
