package ru.digitalhustle.certis.features.transaction.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class InvalidTransactionException(
    message: String,
) : DomainException(message)
