package ru.digitalhustle.certis.features.budget.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.application.service.BudgetConstraintApplicationService
import ru.digitalhustle.certis.features.budget.application.validator.BudgetForecastValidator
import ru.digitalhustle.certis.features.budget.command.model.SaveBudgetConstraintsData
import ru.digitalhustle.certis.features.budget.command.service.BudgetConstraintCommandService
import ru.digitalhustle.certis.features.budget.command.service.BudgetPlanCommandService
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.query.service.BudgetConstraintQueryService
import ru.digitalhustle.certis.features.budget.query.service.BudgetForecastQueryService
import ru.digitalhustle.certis.features.budget.util.BudgetConstraintCalculator
import ru.digitalhustle.certis.features.budget.validation.BudgetConstraintValidator
import ru.digitalhustle.certis.features.category.api.CategoryCommandAccess
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import ru.digitalhustle.certis.util.time.ApplicationClock

@Service
class BudgetConstraintApplicationServiceImpl(
    private val planCommandService: BudgetPlanCommandService,
    private val forecastQueryService: BudgetForecastQueryService,
    private val constraintQueryService: BudgetConstraintQueryService,
    private val constraintCommandService: BudgetConstraintCommandService,
    private val categoryCommandAccess: CategoryCommandAccess,
    private val calculator: BudgetConstraintCalculator,
    private val forecastValidator: BudgetForecastValidator,
    private val constraintValidator: BudgetConstraintValidator,
    private val applicationClock: ApplicationClock,
) : BudgetConstraintApplicationService {

    @Transactional
    override fun save(data: SaveBudgetConstraintsData): BudgetConstraintSet {
        val plan = planCommandService.getByIdForUpdate(data.planId, data.userId)
        forecastValidator.validatePlan(plan, data.expectedVersion)
        val forecast = forecastQueryService.getCurrent(plan.id, plan.userId)
        constraintValidator.validateCurrentForecast(forecast, data.forecastRevision)
        val suggestion = constraintQueryService.getSuggestion(plan, forecast)
        val categories = categoryCommandAccess.getAllByIdsForShare(
            data.categories.map { category -> category.categoryId }.toSet(),
            data.userId,
        ).associateBy(CategorySnapshot::id)
        constraintValidator.validateRequest(data, forecast, suggestion, categories)

        val now = applicationClock.now()
        val nextPlan = planCommandService.incrementVersion(plan)
        val constraints = calculator.confirm(
            suggestion = suggestion,
            forecast = forecast,
            data = data,
            categorySnapshots = categories,
            revision = constraintCommandService.nextRevision(plan.id),
            planVersion = nextPlan.version,
            createdAt = now,
        )
        constraintCommandService.save(constraints)
        constraintCommandService.markGeneratedOptimizationsStale(plan.id, plan.userId, now)
        return constraints
    }
}
