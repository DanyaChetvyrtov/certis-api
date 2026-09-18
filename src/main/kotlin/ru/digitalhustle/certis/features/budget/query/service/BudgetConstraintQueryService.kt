package ru.digitalhustle.certis.features.budget.query.service

import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import java.util.UUID

interface BudgetConstraintQueryService {

    fun getConstraints(planId: UUID, userId: UUID): BudgetConstraintSet

    fun getSuggestion(plan: BudgetPlan, forecast: BudgetForecast): BudgetConstraintSet

    fun findLatest(planId: UUID, userId: UUID): BudgetConstraintSet?
}
