package ru.digitalhustle.certis.features.budget.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.enums.BudgetPriority
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryConstraint
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryFundingLevel
import java.math.BigDecimal

@Component
class BudgetMckpSolver {

    fun solve(
        categories: List<BudgetCategoryConstraint>,
        capacity: BigDecimal,
    ): BudgetMckpSolution {
        val orderedCategories = categories.sortedBy { category -> category.category.id }
        val finalStates = orderedCategories.fold(listOf(BudgetMckpState.EMPTY)) { states, category ->
            paretoFrontier(combine(states, category, capacity), orderedCategories)
        }
        val best = finalStates.reduce { current, candidate ->
            if (better(candidate, current, orderedCategories)) candidate else current
        }
        return BudgetMckpSolution(
            cost = best.cost,
            objectiveValue = best.objectiveValue,
            selections = best.selections,
        )
    }

    private fun combine(
        states: List<BudgetMckpState>,
        category: BudgetCategoryConstraint,
        capacity: BigDecimal,
    ): List<BudgetMckpState> =
        states.flatMap { state ->
            category.fundingLevels
                .sortedBy { level -> level.level.ordinal }
                .mapNotNull { level -> state.add(category, level).takeIf { candidate -> candidate.cost <= capacity } }
        }

    private fun paretoFrontier(
        candidates: List<BudgetMckpState>,
        categories: List<BudgetCategoryConstraint>,
    ): List<BudgetMckpState> =
        candidates.filter { candidate ->
            candidates.none { other ->
                other !== candidate && dominates(other, candidate, categories)
            }
        }

    private fun dominates(
        candidate: BudgetMckpState,
        other: BudgetMckpState,
        categories: List<BudgetCategoryConstraint>,
    ): Boolean {
        if (candidate.cost > other.cost || candidate.objectiveValue < other.objectiveValue) return false
        if (candidate.cost < other.cost || candidate.objectiveValue > other.objectiveValue) return true
        return better(candidate, other, categories)
    }

    private fun better(
        candidate: BudgetMckpState,
        current: BudgetMckpState,
        categories: List<BudgetCategoryConstraint>,
    ): Boolean {
        candidate.objectiveValue.compareTo(current.objectiveValue).takeIf { it != 0 }
            ?.let { return it > 0 }
        candidate.cost.compareTo(current.cost).takeIf { it != 0 }
            ?.let { return it < 0 }
        return preferredSelections(candidate, current, categories)
    }

    private fun preferredSelections(
        candidate: BudgetMckpState,
        current: BudgetMckpState,
        categories: List<BudgetCategoryConstraint>,
    ): Boolean {
        val ordered = categories.sortedWith(
            compareByDescending<BudgetCategoryConstraint> { category ->
                priorityWeight(requireNotNull(category.priority))
            }
                .thenBy { category -> category.category.id },
        )
        ordered.forEach { category ->
            val candidateLevel = candidate.selections[category.category.id] ?: return@forEach
            val currentLevel = current.selections[category.category.id] ?: return@forEach
            candidateLevel.amount.compareTo(currentLevel.amount).takeIf { it != 0 }
                ?.let { return it > 0 }
            candidateLevel.level.ordinal.compareTo(currentLevel.level.ordinal).takeIf { it != 0 }
                ?.let { return it < 0 }
        }
        return false
    }

    private fun BudgetMckpState.add(
        category: BudgetCategoryConstraint,
        level: BudgetCategoryFundingLevel,
    ): BudgetMckpState {
        val priority = requireNotNull(category.priority)
        return BudgetMckpState(
            cost = cost + level.amount - category.requiredAmount,
            objectiveValue = objectiveValue + level.coverage.multiply(priorityWeight(priority)),
            selections = selections + (category.category.id to level),
        )
    }

    private fun priorityWeight(priority: BudgetPriority): BigDecimal =
        when (priority) {
            BudgetPriority.HIGH -> BigDecimal("3")
            BudgetPriority.MEDIUM -> BigDecimal("2")
            BudgetPriority.LOW -> BigDecimal.ONE
        }
}

data class BudgetMckpSolution(
    val cost: BigDecimal,
    val objectiveValue: BigDecimal,
    val selections: Map<java.util.UUID, BudgetCategoryFundingLevel>,
)

private data class BudgetMckpState(
    val cost: BigDecimal,
    val objectiveValue: BigDecimal,
    val selections: Map<java.util.UUID, BudgetCategoryFundingLevel>,
) {
    companion object {
        val EMPTY = BudgetMckpState(BigDecimal.ZERO, BigDecimal.ZERO, emptyMap())
    }
}
