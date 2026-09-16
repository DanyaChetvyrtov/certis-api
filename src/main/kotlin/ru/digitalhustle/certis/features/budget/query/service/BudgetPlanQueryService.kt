package ru.digitalhustle.certis.features.budget.query.service

import ru.digitalhustle.certis.features.budget.model.BudgetPlanRevisions
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.model.BudgetPlanView
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.LocalDate
import java.util.UUID

interface BudgetPlanQueryService {
    fun getEntityById(id: UUID, userId: UUID): BudgetPlan

    fun getCurrent(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ): BudgetPlanView

    fun getById(
        id: UUID,
        userId: UUID,
    ): BudgetPlanView

    fun getRevisions(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ): BudgetPlanRevisions
}
