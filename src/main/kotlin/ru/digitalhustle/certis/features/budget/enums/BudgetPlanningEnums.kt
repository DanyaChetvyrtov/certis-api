package ru.digitalhustle.certis.features.budget.enums

enum class BudgetPlanStatus {
    DRAFT,
    APPLIED,
    SUPERSEDED,
    CANCELLED,
}

enum class BudgetPlanningStep {
    FORECAST,
    CONSTRAINTS,
    OPTIMIZE,
    REVIEW,
    APPLIED,
}

enum class BudgetForecastStatus {
    MISSING,
    CURRENT,
    STALE,
}

enum class BudgetConstraintStatus {
    MISSING,
    SUGGESTED,
    CONFIRMED,
}

enum class BudgetFeasibilityStatus {
    FEASIBLE,
    INFEASIBLE,
}

enum class BudgetOptimizationRunStatus {
    GENERATED,
    INFEASIBLE,
    APPLIED,
    DISMISSED,
    STALE,
}

enum class BudgetPlanningErrorCode {
    BUDGET_PLAN_NOT_FOUND,
    ACTIVE_BUDGET_PLAN_EXISTS,
    IDEMPOTENCY_KEY_REUSED,
}
