package ru.digitalhustle.certis.api.dto

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

enum class BudgetAllocationType {
    FIXED,
    VARIABLE,
}

enum class BudgetConstraintRole {
    REQUIRED,
    FLEXIBLE,
}

enum class BudgetPriority {
    HIGH,
    MEDIUM,
    LOW,
}

enum class BudgetFundingLevel {
    MINIMUM,
    BALANCED,
    COMFORTABLE,
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
    BUDGET_OPTIMIZATION_NOT_FOUND,
    ACTIVE_BUDGET_PLAN_EXISTS,
    PLANNING_VERSION_CONFLICT,
    INVALID_BUDGET_PLAN_STATE,
    FORECAST_SOURCE_CHANGED,
    OPTIMIZATION_STALE,
    BUDGET_PLAN_ALREADY_APPLIED,
    FORECAST_REQUIRED,
    CONSTRAINTS_INCOMPLETE,
    INVALID_FUNDING_LEVELS,
    UNCATEGORIZED_REQUIRED_OCCURRENCE,
    CURRENCY_MISMATCH,
}
