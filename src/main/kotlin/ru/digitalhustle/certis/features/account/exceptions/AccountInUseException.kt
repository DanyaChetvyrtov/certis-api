package ru.digitalhustle.certis.features.account.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class AccountInUseException(
    message: String,
) : DomainException(message)
