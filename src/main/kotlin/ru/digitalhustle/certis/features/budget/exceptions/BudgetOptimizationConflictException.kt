package ru.digitalhustle.certis.features.budget.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException

class BudgetOptimizationConflictException(
    message: String,
) : DomainException(message)
