package ru.digitalhustle.certis.features.goal.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.goal.model.GoalContributionFilter
import ru.digitalhustle.certis.features.goal.model.GoalContributionPage
import ru.digitalhustle.certis.features.goal.model.GoalFilter
import ru.digitalhustle.certis.features.goal.model.GoalOverview
import ru.digitalhustle.certis.features.goal.model.GoalOverviewFilter
import ru.digitalhustle.certis.features.goal.model.GoalPage
import ru.digitalhustle.certis.features.goal.model.GoalPlanPreview
import ru.digitalhustle.certis.features.goal.model.GoalPlanPreviewData
import ru.digitalhustle.certis.features.goal.model.GoalView
import ru.digitalhustle.certis.features.goal.query.service.GoalQueryService
import ru.digitalhustle.certis.features.goal.query.service.GoalReadService
import ru.digitalhustle.certis.features.goal.query.service.GoalTransactionQueryService
import ru.digitalhustle.certis.features.goal.util.GoalCalculator
import ru.digitalhustle.certis.features.goal.util.GoalOverviewFactory
import ru.digitalhustle.certis.features.security.api.UserPreferencesQuery
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
@Transactional(readOnly = true)
class GoalReadServiceImpl(
    private val goalQueryService: GoalQueryService,
    private val goalTransactionService: GoalTransactionQueryService,
    private val userPreferencesQuery: UserPreferencesQuery,
    private val goalCalculator: GoalCalculator,
    private val goalOverviewFactory: GoalOverviewFactory,
    private val applicationClock: ApplicationClock,
) : GoalReadService {

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getPage(
        userId: UUID,
        filter: GoalFilter,
    ): GoalPage {
        val currency = filter.currency ?: userPreferencesQuery.getPreferredCurrency(userId)
        return goalQueryService.getPage(userId, currency, filter)
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getOverview(
        userId: UUID,
        filter: GoalOverviewFilter,
    ): GoalOverview {
        val currency = filter.currency ?: userPreferencesQuery.getPreferredCurrency(userId)
        val month = filter.month ?: applicationClock.currentMonth()
        val allGoals = goalQueryService.getAll(userId, currency)
        val monthlyAmounts = goalTransactionService.getNetAmounts(
            userId = userId,
            currency = currency,
            from = applicationClock.startOfMonth(month),
            to = applicationClock.startOfNextMonth(month),
        )

        return goalOverviewFactory.create(month, currency, allGoals, monthlyAmounts)
    }

    override fun preview(data: GoalPlanPreviewData): GoalPlanPreview = goalCalculator.preview(data)

    @Transactional(readOnly = true)
    override fun getById(
        id: UUID,
        userId: UUID,
    ): GoalView = goalQueryService.getById(id, userId)

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getContributions(
        goalId: UUID,
        userId: UUID,
        filter: GoalContributionFilter,
    ): GoalContributionPage {
        goalQueryService.requireOwned(goalId, userId)
        return goalTransactionService.getPage(goalId, userId, filter)
    }
}
