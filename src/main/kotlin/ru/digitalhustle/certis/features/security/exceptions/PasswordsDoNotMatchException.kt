package ru.digitalhustle.certis.features.security.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class PasswordsDoNotMatchException(
    message: String,
) : DomainException(message)
