package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.budget.BudgetOptimizationDetails
import ru.digitalhustle.certis.model.budget.CalculatedBudgetOptimization
import java.time.LocalDate
import java.util.UUID

interface BudgetOptimizationService {

    fun getLatest(userId: UUID, budgetMonth: LocalDate): BudgetOptimizationDetails

    fun getProposedForUpdate(
        id: UUID,
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetOptimizationDetails

    fun create(
        userId: UUID,
        calculation: CalculatedBudgetOptimization,
    ): BudgetOptimizationDetails

    fun apply(id: UUID, userId: UUID): BudgetOptimizationDetails

    fun dismiss(id: UUID, userId: UUID)
}
