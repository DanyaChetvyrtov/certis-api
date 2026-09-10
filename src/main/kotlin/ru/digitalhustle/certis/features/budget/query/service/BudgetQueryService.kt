package ru.digitalhustle.certis.features.budget.query.service

import ru.digitalhustle.certis.features.budget.model.BudgetDetails
import java.time.LocalDate
import java.util.UUID

interface BudgetQueryService {

    fun getByMonth(userId: UUID, budgetMonth: LocalDate): BudgetDetails
}
