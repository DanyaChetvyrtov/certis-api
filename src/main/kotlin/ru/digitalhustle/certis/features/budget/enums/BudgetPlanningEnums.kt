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

enum class BudgetForecastSourceType {
    RECURRING,
    HISTORICAL_CATEGORY,
    CURRENT_LIMIT,
    MANUAL,
    ACTUAL,
}

enum class BudgetForecastOperationType {
    INCOME,
    EXPENSE,
}

enum class BudgetForecastConfidence {
    HIGH,
    MEDIUM,
    LOW,
}

enum class BudgetConstraintRole {
    REQUIRED,
    FLEXIBLE,
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
    PLANNING_VERSION_CONFLICT,
    INVALID_BUDGET_PLAN_STATE,
    FORECAST_SOURCE_CHANGED,
    FORECAST_REQUIRED,
    IDEMPOTENCY_KEY_REUSED,
}
