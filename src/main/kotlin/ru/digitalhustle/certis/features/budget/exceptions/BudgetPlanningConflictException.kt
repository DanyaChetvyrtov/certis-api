package ru.digitalhustle.certis.features.budget.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode

class BudgetPlanningConflictException(
    message: String,
    val code: BudgetPlanningErrorCode,
    val details: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : DomainException(message, cause)
