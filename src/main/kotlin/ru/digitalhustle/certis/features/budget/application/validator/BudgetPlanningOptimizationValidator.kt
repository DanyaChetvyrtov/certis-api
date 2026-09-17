package ru.digitalhustle.certis.features.budget.application.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.command.model.GenerateBudgetPlanningOptimizationData
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetFeasibilityStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningConflictException
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningValidationException
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun

@Component
class BudgetPlanningOptimizationValidator {

    fun validateInputs(
        data: GenerateBudgetPlanningOptimizationData,
        forecast: BudgetForecast,
        constraints: BudgetConstraintSet?,
    ): BudgetConstraintSet {
        val currentConstraints = constraints ?: throw incompleteConstraints(data)
        validateForecast(data, forecast, currentConstraints)
        validateConstraints(data, currentConstraints)
        validateTargetSavings(data, currentConstraints)
        return currentConstraints
    }

    private fun validateForecast(
        data: GenerateBudgetPlanningOptimizationData,
        forecast: BudgetForecast,
        constraints: BudgetConstraintSet,
    ) {
        if (forecast.status != BudgetForecastStatus.CURRENT) {
            throw conflict(
                message = "Forecast sources changed; confirm a new forecast revision",
                code = BudgetPlanningErrorCode.FORECAST_SOURCE_CHANGED,
                data = data,
            )
        }
        if (
            forecast.revision != data.forecastRevision ||
            constraints.basedOnForecastRevision != data.forecastRevision ||
            constraints.forecastRevisionId != forecast.id
        ) {
            throw conflict(
                message = "Forecast or constraints revision changed; refresh the plan",
                code = BudgetPlanningErrorCode.FORECAST_SOURCE_CHANGED,
                data = data,
            )
        }
    }

    private fun validateConstraints(data: GenerateBudgetPlanningOptimizationData, constraints: BudgetConstraintSet) {
        if (
            constraints.status != BudgetConstraintStatus.CONFIRMED ||
            constraints.revision != data.constraintsRevision ||
            constraints.feasibility.status != BudgetFeasibilityStatus.FEASIBLE
        ) {
            throw incompleteConstraints(data)
        }
    }

    private fun validateTargetSavings(data: GenerateBudgetPlanningOptimizationData, constraints: BudgetConstraintSet) {
        if (data.targetSavingsAmount < constraints.savingsFloorAmount) {
            throw BudgetPlanningValidationException(
                message = "Target savings cannot be below the confirmed savings floor",
                code = BudgetPlanningErrorCode.CONSTRAINTS_INCOMPLETE,
                details = mapOf(
                    "targetSavingsAmount" to data.targetSavingsAmount,
                    "savingsFloorAmount" to constraints.savingsFloorAmount,
                ),
            )
        }
    }

    fun validateIdempotentReplay(
        existing: BudgetPlanningOptimizationRun,
        data: GenerateBudgetPlanningOptimizationData,
    ) {
        val input = existing.input
        val sameCommand = existing.planId == data.planId &&
            input.planVersion == data.expectedVersion &&
            input.forecastRevision == data.forecastRevision &&
            input.constraintsRevision == data.constraintsRevision &&
            input.targetSavingsAmount.compareTo(data.targetSavingsAmount) == 0
        if (!sameCommand) {
            throw conflict(
                message = "Idempotency key is already used for another optimization command",
                code = BudgetPlanningErrorCode.IDEMPOTENCY_KEY_REUSED,
                data = data,
            )
        }
    }

    fun validateDismissible(run: BudgetPlanningOptimizationRun) {
        if (run.status != BudgetOptimizationRunStatus.GENERATED) {
            throw BudgetPlanningConflictException(
                message = "Only a generated optimization can be dismissed",
                code = BudgetPlanningErrorCode.INVALID_BUDGET_PLAN_STATE,
                details = mapOf("optimizationId" to run.id, "status" to run.status.name),
            )
        }
    }

    private fun incompleteConstraints(data: GenerateBudgetPlanningOptimizationData) =
        BudgetPlanningValidationException(
            message = "Current confirmed and feasible constraints are required",
            code = BudgetPlanningErrorCode.CONSTRAINTS_INCOMPLETE,
            details = mapOf(
                "planId" to data.planId,
                "forecastRevision" to data.forecastRevision,
                "constraintsRevision" to data.constraintsRevision,
            ),
        )

    private fun conflict(
        message: String,
        code: BudgetPlanningErrorCode,
        data: GenerateBudgetPlanningOptimizationData,
    ) = BudgetPlanningConflictException(
        message = message,
        code = code,
        details = mapOf("planId" to data.planId, "idempotencyKey" to data.idempotencyKey),
    )
}
