package ru.digitalhustle.certis.features.budget.exceptions

import ru.digitalhustle.certis.exception.custom.DomainException
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode

class BudgetPlanningOptimizationNotFoundException(
    val details: Map<String, Any?> = emptyMap(),
) : DomainException("Budget optimization not found") {
    val code = BudgetPlanningErrorCode.BUDGET_OPTIMIZATION_NOT_FOUND
}
