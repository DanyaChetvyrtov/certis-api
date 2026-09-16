package ru.digitalhustle.certis.features.budget.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.features.budget.command.repository.BudgetForecastItemRepository
import ru.digitalhustle.certis.features.budget.command.repository.BudgetForecastRevisionRepository
import ru.digitalhustle.certis.features.budget.command.repository.BudgetOptimizationRunRepository
import ru.digitalhustle.certis.features.budget.command.service.BudgetForecastCommandService
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BudgetForecastCommandServiceImpl(
    private val revisionRepository: BudgetForecastRevisionRepository,
    private val itemRepository: BudgetForecastItemRepository,
    private val optimizationRunRepository: BudgetOptimizationRunRepository,
) : BudgetForecastCommandService {

    override fun nextRevision(planId: UUID): Int = revisionRepository.nextRevision(planId)

    override fun save(forecast: BudgetForecast) {
        revisionRepository.insert(forecast)
        itemRepository.insertAll(forecast.id, forecast.userId, forecast.items)
    }

    override fun markGeneratedOptimizationsStale(planId: UUID, userId: UUID, staleAt: OffsetDateTime) {
        optimizationRunRepository.markGeneratedStale(planId, userId, staleAt)
    }
}
