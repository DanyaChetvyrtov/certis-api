package ru.digitalhustle.certis.features.budget.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetFeasibilityStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningStep
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.model.BudgetPlanCapabilities
import ru.digitalhustle.certis.features.budget.model.BudgetPlanConstraintState
import ru.digitalhustle.certis.features.budget.model.BudgetPlanFeasibility
import ru.digitalhustle.certis.features.budget.model.BudgetPlanForecastState
import ru.digitalhustle.certis.features.budget.model.BudgetPlanForecastSummary
import ru.digitalhustle.certis.features.budget.model.BudgetPlanOptimizationState
import ru.digitalhustle.certis.features.budget.model.BudgetPlanRevision
import ru.digitalhustle.certis.features.budget.model.BudgetPlanStateSnapshot
import ru.digitalhustle.certis.features.budget.model.BudgetPlanView

@Component
class BudgetPlanViewFactory {

    fun create(
        plan: BudgetPlan,
        state: BudgetPlanStateSnapshot,
    ): BudgetPlanView =
        BudgetPlanView(
            id = plan.id,
            previousPlanId = plan.previousPlanId,
            budgetMonth = plan.budgetMonth,
            currency = plan.currency,
            revision = plan.revision,
            status = plan.status,
            currentStep = currentStep(plan, state),
            version = plan.version,
            forecast = forecastState(state),
            constraints = constraintState(state),
            currentOptimization = state.optimization?.let { optimization ->
                BudgetPlanOptimizationState(
                    id = optimization.id,
                    status = optimization.status,
                    targetSavingsAmount = optimization.targetSavingsAmount,
                    actualSavingsAmount = optimization.actualSavingsAmount,
                    createdAt = optimization.createdAt,
                )
            },
            capabilities = capabilities(plan, state),
            createdAt = plan.createdAt,
            updatedAt = plan.updatedAt,
        )

    fun createRevision(
        plan: BudgetPlan,
        state: BudgetPlanStateSnapshot,
    ): BudgetPlanRevision =
        BudgetPlanRevision(
            id = plan.id,
            revision = plan.revision,
            status = plan.status,
            currentStep = currentStep(plan, state),
            createdAt = plan.createdAt,
            appliedAt = plan.appliedAt,
        )

    private fun forecastState(state: BudgetPlanStateSnapshot): BudgetPlanForecastState {
        val forecast = state.forecast
            ?: return BudgetPlanForecastState(
                revision = null,
                status = BudgetForecastStatus.MISSING,
                summary = null,
            )

        return BudgetPlanForecastState(
            revision = forecast.revision,
            status = BudgetForecastStatus.CURRENT,
            summary = BudgetPlanForecastSummary(
                forecastIncome = forecast.forecastIncome,
                recurringExpenses = forecast.recurringExpenses,
                flexibleEstimate = forecast.flexibleEstimate,
                forecastExpenses = forecast.forecastExpenses,
                forecastSavings = forecast.forecastSavings,
                includedItemCount = forecast.includedItemCount,
                excludedItemCount = forecast.excludedItemCount,
            ),
        )
    }

    private fun constraintState(state: BudgetPlanStateSnapshot): BudgetPlanConstraintState {
        val constraints = currentConstraints(state)
            ?: return BudgetPlanConstraintState(
                revision = null,
                status = BudgetConstraintStatus.MISSING,
                feasibility = null,
            )
        val forecastIncome = requireNotNull(state.forecast).forecastIncome

        return BudgetPlanConstraintState(
            revision = constraints.revision,
            status = BudgetConstraintStatus.CONFIRMED,
            feasibility = BudgetPlanFeasibility(
                status = constraints.status,
                forecastIncome = forecastIncome,
                requiredAmount = constraints.requiredAmount,
                variableMinimumAmount = constraints.variableMinimumAmount,
                savingsFloorAmount = constraints.savingsFloorAmount,
                maximumSavingsAmount = constraints.maximumSavingsAmount,
                capacityAtSavingsFloor = forecastIncome - constraints.requiredAmount - constraints.savingsFloorAmount,
                shortfall = constraints.shortfall,
                violations = constraints.violations,
            ),
        )
    }

    private fun currentStep(
        plan: BudgetPlan,
        state: BudgetPlanStateSnapshot,
    ): BudgetPlanningStep =
        when {
            plan.status == BudgetPlanStatus.APPLIED || plan.status == BudgetPlanStatus.SUPERSEDED ->
                BudgetPlanningStep.APPLIED
            state.forecast == null -> BudgetPlanningStep.FORECAST
            currentConstraints(state)?.status != BudgetFeasibilityStatus.FEASIBLE -> BudgetPlanningStep.CONSTRAINTS
            state.optimization == null ||
                state.optimization.status == BudgetOptimizationRunStatus.INFEASIBLE ||
                state.optimization.status == BudgetOptimizationRunStatus.DISMISSED ||
                state.optimization.status == BudgetOptimizationRunStatus.STALE -> BudgetPlanningStep.OPTIMIZE
            else -> BudgetPlanningStep.REVIEW
        }

    private fun capabilities(
        plan: BudgetPlan,
        state: BudgetPlanStateSnapshot,
    ): BudgetPlanCapabilities {
        val editable = plan.status == BudgetPlanStatus.DRAFT

        return BudgetPlanCapabilities(
            canEditForecast = editable,
            canEditConstraints = editable && state.forecast != null,
            canRunOptimization = editable &&
                currentConstraints(state)?.status == BudgetFeasibilityStatus.FEASIBLE,
            canApply = editable && state.optimization?.status == BudgetOptimizationRunStatus.GENERATED,
            canCancel = editable,
        )
    }

    private fun currentConstraints(state: BudgetPlanStateSnapshot) =
        state.constraints?.takeIf { constraints -> constraints.forecastRevision == state.forecast?.revision }
}
