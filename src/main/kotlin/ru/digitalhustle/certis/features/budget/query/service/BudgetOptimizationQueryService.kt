package ru.digitalhustle.certis.features.budget.query.service

import ru.digitalhustle.certis.features.budget.model.BudgetOptimizationDetails
import java.time.LocalDate
import java.util.UUID

interface BudgetOptimizationQueryService {

    fun getLatest(userId: UUID, budgetMonth: LocalDate): BudgetOptimizationDetails
}
