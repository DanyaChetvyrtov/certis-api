package ru.digitalhustle.certis.features.budget.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationType
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPriority
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryConstraint
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningConstraintCheck
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationDecision
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationInput
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationReason
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationResult
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningViolation
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID

@Component
class BudgetPlanningOptimizationCalculator(
    private val solver: BudgetMckpSolver,
    private val fingerprintCalculator: BudgetPlanningOptimizationFingerprintCalculator,
) {

    @Suppress("LongParameterList")
    fun calculate(
        plan: BudgetPlan,
        resultPlanVersion: Long,
        forecast: BudgetForecast,
        constraints: BudgetConstraintSet,
        targetSavingsAmount: BigDecimal,
        idempotencyKey: String,
        createdAt: OffsetDateTime,
    ): BudgetPlanningOptimizationRun {
        val input = input(plan, forecast, constraints, targetSavingsAmount)
        val fingerprint = fingerprintCalculator.calculate(forecast, constraints, targetSavingsAmount)
        if (targetSavingsAmount > input.maximumSavingsAmount) {
            return infeasibleRun(plan, constraints, input, fingerprint, idempotencyKey, resultPlanVersion, createdAt)
        }
        val variableCategories = constraints.categories.variable()
        val solution = solver.solve(variableCategories, input.flexibleCapacity)
        val result = result(input, variableCategories, solution)
        return BudgetPlanningOptimizationRun(
            id = UUID.randomUUID(),
            userId = plan.userId,
            planId = plan.id,
            forecastRevisionId = forecast.id,
            constraintRevisionId = requireNotNull(constraints.id),
            status = BudgetOptimizationRunStatus.GENERATED,
            algorithmVersion = BudgetPlanningOptimizationFingerprintCalculator.ALGORITHM_VERSION,
            generationIdempotencyKey = idempotencyKey,
            inputFingerprint = fingerprint,
            input = input,
            result = result,
            decisions = decisions(constraints.categories, solution),
            constraintChecks = checks(input, result),
            violations = emptyList(),
            planVersion = resultPlanVersion,
            createdAt = createdAt,
        )
    }

    private fun input(
        plan: BudgetPlan,
        forecast: BudgetForecast,
        constraints: BudgetConstraintSet,
        targetSavingsAmount: BigDecimal,
    ): BudgetPlanningOptimizationInput {
        val feasibility = constraints.feasibility
        val variableCategories = constraints.categories.variable()
        return BudgetPlanningOptimizationInput(
            planVersion = plan.version,
            forecastRevision = forecast.revision,
            constraintsRevision = requireNotNull(constraints.revision),
            forecastIncome = forecast.summary.forecastIncome,
            requiredAmount = feasibility.requiredAmount,
            savingsFloorAmount = constraints.savingsFloorAmount,
            targetSavingsAmount = targetSavingsAmount,
            maximumSavingsAmount = feasibility.maximumSavingsAmount,
            flexibleCapacity = forecast.summary.forecastIncome - feasibility.requiredAmount - targetSavingsAmount,
            flexibleCategoryCount = variableCategories.size,
            candidateOptionCount = variableCategories.sumOf { category -> category.fundingLevels.size },
            forecastFingerprint = forecast.forecastFingerprint,
            constraintFingerprint = requireNotNull(constraints.constraintFingerprint),
            baselineSavingsAmount = forecast.summary.forecastIncome - constraints.categories.sumCurrentLimits(),
        )
    }

    @Suppress("LongParameterList")
    private fun infeasibleRun(
        plan: BudgetPlan,
        constraints: BudgetConstraintSet,
        input: BudgetPlanningOptimizationInput,
        fingerprint: String,
        idempotencyKey: String,
        planVersion: Long,
        createdAt: OffsetDateTime,
    ): BudgetPlanningOptimizationRun {
        val shortfall = input.targetSavingsAmount - input.maximumSavingsAmount
        return BudgetPlanningOptimizationRun(
            id = UUID.randomUUID(),
            userId = plan.userId,
            planId = plan.id,
            forecastRevisionId = constraints.forecastRevisionId,
            constraintRevisionId = requireNotNull(constraints.id),
            status = BudgetOptimizationRunStatus.INFEASIBLE,
            algorithmVersion = BudgetPlanningOptimizationFingerprintCalculator.ALGORITHM_VERSION,
            generationIdempotencyKey = idempotencyKey,
            inputFingerprint = fingerprint,
            input = input,
            result = null,
            decisions = emptyList(),
            constraintChecks = emptyList(),
            violations = listOf(
                BudgetPlanningViolation(
                    code = MINIMUM_ALLOCATION_EXCEEDS_CAPACITY,
                    shortfall = shortfall,
                    requiredAmount = constraints.feasibility.variableMinimumAmount,
                    availableAmount = input.flexibleCapacity,
                ),
            ),
            planVersion = planVersion,
            createdAt = createdAt,
        )
    }

    private fun result(
        input: BudgetPlanningOptimizationInput,
        variableCategories: List<BudgetCategoryConstraint>,
        solution: BudgetMckpSolution,
    ): BudgetPlanningOptimizationResult {
        val totalAllocation = input.requiredAmount + solution.cost
        val actualSavings = input.forecastIncome - totalAllocation
        val priorityTotal = variableCategories.fold(BigDecimal.ZERO) { total, category ->
            total + priorityWeight(requireNotNull(category.priority))
        }
        val coverage = if (priorityTotal.signum() == 0) {
            BigDecimal.ONE
        } else {
            solution.objectiveValue.divide(priorityTotal, SCORE_SCALE, RoundingMode.HALF_UP)
        }
        return BudgetPlanningOptimizationResult(
            requiredAllocation = input.requiredAmount,
            flexibleAllocation = solution.cost,
            totalAllocation = totalAllocation,
            targetSavings = input.targetSavingsAmount,
            actualSavings = actualSavings,
            additionalSavingsComparedWithCurrent = actualSavings - input.baselineSavingsAmount,
            coverageScore = coverage,
            objectiveValue = solution.objectiveValue,
            unusedCapacity = actualSavings - input.targetSavingsAmount,
        )
    }

    private fun decisions(
        categories: List<BudgetCategoryConstraint>,
        solution: BudgetMckpSolution,
    ): List<BudgetPlanningOptimizationDecision> =
        categories.sortedBy { category -> category.category.name }.map { category ->
            if (category.allocationType == BudgetAllocationType.FIXED) {
                fixedDecision(category)
            } else {
                variableDecision(category, requireNotNull(solution.selections[category.category.id]))
            }
        }

    private fun fixedDecision(category: BudgetCategoryConstraint): BudgetPlanningOptimizationDecision =
        BudgetPlanningOptimizationDecision(
            id = UUID.randomUUID(),
            categoryConstraintId = category.id,
            fundingLevelId = null,
            category = category.category,
            allocationType = category.allocationType,
            constraintRole = category.constraintRole,
            priority = null,
            currentLimit = category.currentLimitAmount,
            requiredAmount = category.requiredAmount,
            selectedLevel = null,
            recommendedLimit = category.requiredAmount,
            change = category.requiredAmount - category.currentLimitAmount,
            coverage = BigDecimal.ONE,
            optionValue = null,
            reason = BudgetPlanningOptimizationReason(
                code = REQUIRED_AMOUNT_PROTECTED,
                parameters = mapOf("sourceCount" to category.sourceKeys.size),
            ),
        )

    private fun variableDecision(
        category: BudgetCategoryConstraint,
        level: ru.digitalhustle.certis.features.budget.model.BudgetCategoryFundingLevel,
    ): BudgetPlanningOptimizationDecision {
        val priority = requireNotNull(category.priority)
        return BudgetPlanningOptimizationDecision(
            id = UUID.randomUUID(),
            categoryConstraintId = category.id,
            fundingLevelId = level.id,
            category = category.category,
            allocationType = category.allocationType,
            constraintRole = category.constraintRole,
            priority = priority,
            currentLimit = category.currentLimitAmount,
            requiredAmount = category.requiredAmount,
            selectedLevel = level.level,
            recommendedLimit = level.amount,
            change = level.amount - category.currentLimitAmount,
            coverage = level.coverage,
            optionValue = level.coverage.multiply(priorityWeight(priority)),
            reason = BudgetPlanningOptimizationReason(
                code = PRIORITY_WEIGHTED_LEVEL_SELECTED,
                parameters = mapOf("priority" to priority.name, "selectedLevel" to level.level.name),
            ),
        )
    }

    private fun checks(
        input: BudgetPlanningOptimizationInput,
        result: BudgetPlanningOptimizationResult,
    ): List<BudgetPlanningConstraintCheck> =
        listOf(
            check(REQUIRED_PAYMENTS_FUNDED, result.requiredAllocation, input.requiredAmount, true),
            check(SAVINGS_TARGET_REACHED, result.actualSavings, input.targetSavingsAmount, true),
            check(ALLOCATION_WITHIN_CAPACITY, result.flexibleAllocation, input.flexibleCapacity, false),
        )

    private fun check(
        code: String,
        actual: BigDecimal,
        required: BigDecimal,
        minimum: Boolean,
    ): BudgetPlanningConstraintCheck =
        BudgetPlanningConstraintCheck(
            code = code,
            satisfied = if (minimum) actual >= required else actual <= required,
            actual = actual,
            required = required,
        )

    private fun List<BudgetCategoryConstraint>.variable(): List<BudgetCategoryConstraint> =
        filter { category -> category.allocationType == BudgetAllocationType.VARIABLE }

    private fun List<BudgetCategoryConstraint>.sumCurrentLimits(): BigDecimal =
        fold(BigDecimal.ZERO) { total, category -> total + category.currentLimitAmount }

    private fun priorityWeight(priority: BudgetPriority): BigDecimal =
        when (priority) {
            BudgetPriority.HIGH -> BigDecimal("3")
            BudgetPriority.MEDIUM -> BigDecimal("2")
            BudgetPriority.LOW -> BigDecimal.ONE
        }

    companion object {
        private const val SCORE_SCALE = 8
        private const val MINIMUM_ALLOCATION_EXCEEDS_CAPACITY = "MINIMUM_ALLOCATION_EXCEEDS_CAPACITY"
        private const val REQUIRED_AMOUNT_PROTECTED = "REQUIRED_AMOUNT_PROTECTED"
        private const val PRIORITY_WEIGHTED_LEVEL_SELECTED = "PRIORITY_WEIGHTED_LEVEL_SELECTED"
        private const val REQUIRED_PAYMENTS_FUNDED = "REQUIRED_PAYMENTS_FUNDED"
        private const val SAVINGS_TARGET_REACHED = "SAVINGS_TARGET_REACHED"
        private const val ALLOCATION_WITHIN_CAPACITY = "ALLOCATION_WITHIN_CAPACITY"
    }
}
