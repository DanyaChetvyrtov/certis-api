package ru.digitalhustle.certis.features.budget.constants

object BudgetErrorMessages {

    const val BUDGET_ALLOCATIONS_EXCEED_INCOME = "Allocations and savings target must not exceed monthly income"
    const val BUDGET_DUPLICATE_CATEGORIES = "Each category can be allocated only once per budget"
    const val BUDGET_CATEGORY_INVALID = "Budget categories must be active expense categories owned by the user"
    const val BUDGET_CONSTRAINT_VIOLATION = "Budget violates allocation constraints"
    const val BUDGET_OPTIMIZATION_NOT_PROPOSED = "Only a proposed budget optimization can be changed"
    const val BUDGET_OPTIMIZATION_STALE = "Budget has changed since the optimization was generated"
}
