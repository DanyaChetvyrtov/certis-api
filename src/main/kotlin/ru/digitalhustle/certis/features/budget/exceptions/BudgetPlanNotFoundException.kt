package ru.digitalhustle.certis.features.budget.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode

class BudgetPlanNotFoundException(
    val details: Map<String, Any?> = emptyMap(),
) : DomainException("Budget plan not found") {
    val code = BudgetPlanningErrorCode.BUDGET_PLAN_NOT_FOUND
}
