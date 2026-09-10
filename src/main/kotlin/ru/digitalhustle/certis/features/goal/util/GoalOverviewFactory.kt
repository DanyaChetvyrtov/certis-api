package ru.digitalhustle.certis.features.goal.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.goal.enums.GoalPaceStatus
import ru.digitalhustle.certis.features.goal.enums.GoalRecommendationType
import ru.digitalhustle.certis.features.goal.enums.GoalStatus
import ru.digitalhustle.certis.features.goal.model.GoalCurrentMonth
import ru.digitalhustle.certis.features.goal.model.GoalMonthlyAmount
import ru.digitalhustle.certis.features.goal.model.GoalMonthlyContribution
import ru.digitalhustle.certis.features.goal.model.GoalOverview
import ru.digitalhustle.certis.features.goal.model.GoalOverviewSummary
import ru.digitalhustle.certis.features.goal.model.GoalRecommendation
import ru.digitalhustle.certis.features.goal.model.GoalView
import ru.digitalhustle.certis.features.goal.model.NearestGoalTarget
import java.math.BigDecimal
import java.time.YearMonth

@Component
class GoalOverviewFactory(
    private val goalCalculator: GoalCalculator,
) {

    fun create(
        month: YearMonth,
        currency: Currency,
        allGoals: List<GoalView>,
        monthlyAmounts: List<GoalMonthlyAmount>,
    ): GoalOverview {
        val activeGoals = allGoals.filter { goal -> goal.status == GoalStatus.ACTIVE }
        val contributedAmount = monthlyAmounts.fold(BigDecimal.ZERO) { total, item -> total + item.amount }
        val plannedAmount = activeGoals.fold(BigDecimal.ZERO) { total, goal ->
            total + goal.contributionPlan.monthlyAmount
        }

        return GoalOverview(
            month = month.toString(),
            currency = currency,
            summary = summary(activeGoals, plannedAmount, contributedAmount),
            nearestTarget = nearestTarget(activeGoals),
            currentMonth = currentMonth(allGoals, monthlyAmounts, plannedAmount, contributedAmount),
            recommendation = recommendation(activeGoals),
        )
    }

    private fun summary(
        goals: List<GoalView>,
        plannedAmount: BigDecimal,
        contributedAmount: BigDecimal,
    ): GoalOverviewSummary =
        GoalOverviewSummary(
            totalSavedAmount =
                goalCalculator.money(goals.fold(BigDecimal.ZERO) { sum, goal -> sum + goal.savedAmount }),
            contributedThisMonthAmount = goalCalculator.money(contributedAmount),
            plannedMonthlyAmount = goalCalculator.money(plannedAmount),
            monthlyPlanCompletionPercentage = percentage(contributedAmount, plannedAmount),
            activeGoalCount = goals.size,
            healthyGoalCount = goals.count { goal -> goal.paceStatus != GoalPaceStatus.ADJUST_PLAN },
            attentionGoalCount = goals.count { goal -> goal.paceStatus == GoalPaceStatus.ADJUST_PLAN },
        )

    private fun currentMonth(
        allGoals: List<GoalView>,
        monthlyAmounts: List<GoalMonthlyAmount>,
        plannedAmount: BigDecimal,
        contributedAmount: BigDecimal,
    ): GoalCurrentMonth {
        val goalById = allGoals.associateBy(GoalView::id)
        val contributions = monthlyAmounts.mapNotNull { item ->
            goalById[item.goalId]?.let { goal ->
                GoalMonthlyContribution(
                    goalId = goal.id,
                    goalName = goal.name,
                    amount = goalCalculator.money(item.amount),
                    color = goal.color,
                )
            }
        }.filter { item -> item.amount.signum() != 0 }
            .sortedByDescending(GoalMonthlyContribution::amount)

        return GoalCurrentMonth(
            plannedAmount = goalCalculator.money(plannedAmount),
            contributedAmount = goalCalculator.money(contributedAmount),
            remainingAmount = goalCalculator.money((plannedAmount - contributedAmount).max(BigDecimal.ZERO)),
            progressPercentage = percentage(contributedAmount, plannedAmount),
            contributions = contributions,
        )
    }

    private fun nearestTarget(goals: List<GoalView>): NearestGoalTarget? =
        goals.filter { goal -> goal.targetMonth != null && goal.monthsRemaining != null }
            .minByOrNull { goal -> goal.targetMonth!! }
            ?.let { goal ->
                NearestGoalTarget(
                    goalId = goal.id,
                    goalName = goal.name,
                    targetMonth = goal.targetMonth!!,
                    monthsRemaining = goal.monthsRemaining!!,
                    targetAmount = goal.targetAmount,
                    savedAmount = goal.savedAmount,
                    remainingAmount = goal.remainingAmount,
                    progressPercentage = goal.progressPercentage,
                    monthlyContributionAmount = goal.contributionPlan.monthlyAmount,
                    paceStatus = goal.paceStatus,
                    icon = goal.icon,
                    color = goal.color,
                )
            }

    private fun recommendation(goals: List<GoalView>): GoalRecommendation? =
        goals.mapNotNull { goal ->
            val targetMonth = goal.targetMonth ?: return@mapNotNull null
            val difference = goal.contributionPlan.recommendedMonthlyAmount - goal.contributionPlan.monthlyAmount
            if (difference.signum() <= 0) {
                return@mapNotNull null
            }
            GoalRecommendation(
                type = GoalRecommendationType.INCREASE_MONTHLY_CONTRIBUTION,
                goalId = goal.id,
                goalName = goal.name,
                targetMonth = targetMonth,
                currentMonthlyAmount = goal.contributionPlan.monthlyAmount,
                recommendedMonthlyAmount = goal.contributionPlan.recommendedMonthlyAmount,
                differenceAmount = goalCalculator.money(difference),
            )
        }.maxByOrNull(GoalRecommendation::differenceAmount)

    private fun percentage(
        actual: BigDecimal,
        planned: BigDecimal,
    ): BigDecimal =
        if (planned.signum() <= 0) {
            goalCalculator.money(BigDecimal.ZERO)
        } else {
            goalCalculator.progress(actual, planned)
        }
}
