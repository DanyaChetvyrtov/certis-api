package ru.digitalhustle.certis.features.budget.application.service

import ru.digitalhustle.certis.features.budget.model.BudgetDetails
import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationDetails
import java.time.LocalDate
import java.util.UUID

interface BudgetOptimizationApplicationService {

    fun generate(userId: UUID, budgetMonth: LocalDate): BudgetOptimizationDetails

    fun apply(
        id: UUID,
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetDetails

    fun dismiss(
        id: UUID,
        userId: UUID,
        budgetMonth: LocalDate,
    )
}
