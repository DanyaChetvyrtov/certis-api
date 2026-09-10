package ru.digitalhustle.certis.features.security.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class MissedTokenException(
    message: String,
) : DomainException(message)
