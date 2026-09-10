package ru.digitalhustle.certis.features.budget.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class InvalidBudgetException(
    message: String,
    cause: Throwable? = null,
) : DomainException(message, cause)
