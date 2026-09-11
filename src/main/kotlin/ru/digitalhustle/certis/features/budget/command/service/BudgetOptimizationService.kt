package ru.digitalhustle.certis.features.budget.command.service

import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationDetails
import ru.digitalhustle.certis.features.budget.model.CalculatedBudgetOptimization
import java.util.UUID

interface BudgetOptimizationService {

    fun getProposedForUpdate(
        id: UUID,
        userId: UUID,
        budgetId: UUID,
    ): BudgetOptimizationDetails

    fun create(
        userId: UUID,
        calculation: CalculatedBudgetOptimization,
    ): BudgetOptimizationDetails

    fun apply(id: UUID, userId: UUID): BudgetOptimizationDetails

    fun dismiss(id: UUID, userId: UUID)
}
