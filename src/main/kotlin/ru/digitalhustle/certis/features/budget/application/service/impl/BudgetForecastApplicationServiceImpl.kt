package ru.digitalhustle.certis.features.budget.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.application.service.BudgetForecastApplicationService
import ru.digitalhustle.certis.features.budget.application.validator.BudgetForecastValidator
import ru.digitalhustle.certis.features.budget.command.model.ConfirmBudgetForecastData
import ru.digitalhustle.certis.features.budget.command.service.BudgetForecastCommandService
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanCommandService
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.query.service.BudgetForecastQueryService
import ru.digitalhustle.certis.features.budget.util.BudgetForecastCalculator
import ru.digitalhustle.certis.features.category.api.CategoryCommandAccess
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
class BudgetForecastApplicationServiceImpl(
    private val planCommandService: BudgetPlanCommandService,
    private val forecastCommandService: BudgetForecastCommandService,
    private val forecastQueryService: BudgetForecastQueryService,
    private val categoryCommandAccess: CategoryCommandAccess,
    private val calculator: BudgetForecastCalculator,
    private val validator: BudgetForecastValidator,
    private val applicationClock: ApplicationClock,
) : BudgetForecastApplicationService {

    @Transactional
    override fun confirm(data: ConfirmBudgetForecastData): BudgetForecast {
        val plan = planCommandService.getByIdForUpdate(data.planId, data.userId)
        validator.validatePlan(plan, data.expectedVersion)
        val preview = forecastQueryService.getPreview(plan)
        validator.validateSourceFingerprint(preview, data.sourceFingerprint)
        validator.validateOverrides(preview, data.overrides)

        val categories = loadCategories(data)
        validator.validateManualAdjustments(preview, data.manualAdjustments, categories)
        val now = applicationClock.now()
        val nextPlan = planCommandService.incrementVersion(plan)
        val previous = forecastQueryService.findLatest(plan.id, plan.userId)
        val forecast = calculator.confirm(
            preview = preview,
            overrides = data.overrides,
            manualAdjustments = data.manualAdjustments,
            categories = categories.mapValues { (_, category) -> category.toForecastCategory() },
            userId = plan.userId,
            revision = forecastCommandService.nextRevision(plan.id),
            planVersion = nextPlan.version,
            confirmedAt = now,
        )
        forecastCommandService.save(forecast)
        if (previous?.forecastFingerprint != forecast.forecastFingerprint) {
            forecastCommandService.markGeneratedOptimizationsStale(plan.id, plan.userId, now)
        }
        return forecast
    }

    private fun loadCategories(data: ConfirmBudgetForecastData): Map<UUID, CategorySnapshot> {
        val categoryIds = data.manualAdjustments.mapNotNull { it.categoryId }.toSet()
        if (categoryIds.isEmpty()) return emptyMap()
        return categoryCommandAccess.getAllByIdsForShare(categoryIds, data.userId)
            .associateBy(CategorySnapshot::id)
    }

    private fun CategorySnapshot.toForecastCategory(): BudgetForecastCategory =
        BudgetForecastCategory(
            id = id,
            type = type,
            name = name,
            icon = icon,
            color = color,
        )
}
