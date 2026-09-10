package ru.digitalhustle.certis.features.transaction.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class InvalidRecurringTransactionException(
    message: String,
) : DomainException(message)
