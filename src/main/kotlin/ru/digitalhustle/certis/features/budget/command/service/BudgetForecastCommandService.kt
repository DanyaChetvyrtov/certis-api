package ru.digitalhustle.certis.features.budget.command.service

import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import java.time.OffsetDateTime
import java.util.UUID

interface BudgetForecastCommandService {
    fun nextRevision(planId: UUID): Int

    fun save(forecast: BudgetForecast)

    fun markGeneratedOptimizationsStale(planId: UUID, userId: UUID, staleAt: OffsetDateTime)
}
