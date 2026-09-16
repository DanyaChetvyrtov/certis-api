package ru.digitalhustle.certis.features.budget.query.service

import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetForecastPreview
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import java.util.UUID

interface BudgetForecastQueryService {
    fun getPreview(planId: UUID, userId: UUID): BudgetForecastPreview

    fun getPreview(plan: BudgetPlan): BudgetForecastPreview

    fun findLatest(planId: UUID, userId: UUID): BudgetForecast?

    fun getCurrent(planId: UUID, userId: UUID): BudgetForecast
}
