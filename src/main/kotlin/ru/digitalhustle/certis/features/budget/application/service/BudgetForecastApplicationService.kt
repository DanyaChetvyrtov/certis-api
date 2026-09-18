package ru.digitalhustle.certis.features.budget.application.service

import ru.digitalhustle.certis.features.budget.command.model.ConfirmBudgetForecastData
import ru.digitalhustle.certis.features.budget.model.BudgetForecast

interface BudgetForecastApplicationService {
    fun confirm(data: ConfirmBudgetForecastData): BudgetForecast
}
