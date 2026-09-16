package ru.digitalhustle.certis.features.budget.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningValidationException
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetForecastPreview
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.query.repository.BudgetForecastQueryRepository
import ru.digitalhustle.certis.features.budget.query.service.BudgetForecastQueryService
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanQueryService
import ru.digitalhustle.certis.features.budget.spi.BudgetForecastSourceProvider
import ru.digitalhustle.certis.features.budget.util.BudgetForecastCalculator
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.YearMonth
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BudgetForecastQueryServiceImpl(
    private val planQueryService: BudgetPlanQueryService,
    private val repository: BudgetForecastQueryRepository,
    private val sourceProvider: BudgetForecastSourceProvider,
    private val calculator: BudgetForecastCalculator,
    private val applicationClock: ApplicationClock,
) : BudgetForecastQueryService {

    override fun getPreview(planId: UUID, userId: UUID): BudgetForecastPreview =
        getPreview(planQueryService.getEntityById(planId, userId))

    override fun getPreview(plan: BudgetPlan): BudgetForecastPreview {
        val previous = repository.findLatestByPlanIdAndUserId(plan.id, plan.userId)
        val sources = sourceProvider.load(
            userId = plan.userId,
            currency = plan.currency,
            targetMonth = YearMonth.from(plan.budgetMonth),
            historyMonths = HISTORY_MONTHS,
        )
        return calculator.preview(
            plan = plan,
            sources = sources,
            baseline = repository.findBaselineAllocations(plan.baselineBudgetId, plan.userId),
            previous = previous,
            generatedAt = applicationClock.now(),
        )
    }

    override fun findLatest(planId: UUID, userId: UUID): BudgetForecast? =
        repository.findLatestByPlanIdAndUserId(planId, userId)

    override fun getCurrent(planId: UUID, userId: UUID): BudgetForecast {
        val plan = planQueryService.getEntityById(planId, userId)
        val forecast = repository.findLatestByPlanIdAndUserId(planId, userId)
            ?: throw BudgetPlanningValidationException(
                message = "A confirmed forecast is required",
                code = BudgetPlanningErrorCode.FORECAST_REQUIRED,
                details = mapOf("planId" to planId),
            )
        val currentSourceFingerprint = getPreview(plan).sourceFingerprint
        return forecast.copy(
            status = if (forecast.sourceFingerprint == currentSourceFingerprint) {
                BudgetForecastStatus.CURRENT
            } else {
                BudgetForecastStatus.STALE
            },
            planVersion = plan.version,
        )
    }

    companion object {
        private const val HISTORY_MONTHS = 6
    }
}
