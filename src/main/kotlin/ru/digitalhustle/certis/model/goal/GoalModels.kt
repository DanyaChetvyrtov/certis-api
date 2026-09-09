package ru.digitalhustle.certis.model.goal

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalContributionPlanType
import ru.digitalhustle.certis.enums.GoalPaceStatus
import ru.digitalhustle.certis.enums.GoalRecommendationType
import ru.digitalhustle.certis.enums.GoalStatus
import ru.digitalhustle.certis.model.entity.Goal
import ru.digitalhustle.certis.model.entity.GoalTransaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class GoalView(
    val id: UUID,
    val name: String,
    val currency: Currency,
    val targetAmount: BigDecimal,
    val savedAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val progressPercentage: BigDecimal,
    val targetMonth: String?,
    val monthsRemaining: Int?,
    val contributionPlan: GoalContributionPlanView,
    val status: GoalStatus,
    val paceStatus: GoalPaceStatus,
    val projectedCompletionMonth: String?,
    val icon: String,
    val color: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val achievedAt: OffsetDateTime?,
    val archivedAt: OffsetDateTime?,
)

data class GoalWithBalance(
    val goal: Goal,
    val savedAmount: BigDecimal,
)

data class GoalContributionPlanView(
    val type: GoalContributionPlanType,
    val monthlyAmount: BigDecimal,
    val recommendedMonthlyAmount: BigDecimal,
)

data class GoalPage(
    val currency: Currency,
    val items: List<GoalView>,
    val statusCounts: GoalStatusCounts,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    val totalPages: Int =
        if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
}

data class GoalStatusCounts(
    val active: Long,
    val completed: Long,
)

data class GoalPlanPreview(
    val currency: Currency,
    val targetAmount: BigDecimal,
    val initialAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val progressPercentage: BigDecimal,
    val targetMonth: String,
    val contributionMonths: Int,
    val recommendedMonthlyAmount: BigDecimal,
    val selectedMonthlyAmount: BigDecimal,
    val projectedCompletionMonth: String?,
    val paceStatus: GoalPaceStatus,
)

data class GoalContributionPage(
    val items: List<GoalTransaction>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    val totalPages: Int =
        if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
}

data class GoalContributionResult(
    val contribution: GoalTransaction,
    val goal: GoalProgress,
)

data class GoalProgress(
    val id: UUID,
    val savedAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val progressPercentage: BigDecimal,
    val status: GoalStatus,
    val paceStatus: GoalPaceStatus,
)

data class GoalOverview(
    val month: String,
    val currency: Currency,
    val summary: GoalOverviewSummary,
    val nearestTarget: NearestGoalTarget?,
    val currentMonth: GoalCurrentMonth,
    val recommendation: GoalRecommendation?,
)

data class GoalOverviewSummary(
    val totalSavedAmount: BigDecimal,
    val contributedThisMonthAmount: BigDecimal,
    val plannedMonthlyAmount: BigDecimal,
    val monthlyPlanCompletionPercentage: BigDecimal,
    val activeGoalCount: Int,
    val healthyGoalCount: Int,
    val attentionGoalCount: Int,
)

data class NearestGoalTarget(
    val goalId: UUID,
    val goalName: String,
    val targetMonth: String,
    val monthsRemaining: Int,
    val targetAmount: BigDecimal,
    val savedAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val progressPercentage: BigDecimal,
    val monthlyContributionAmount: BigDecimal,
    val paceStatus: GoalPaceStatus,
    val icon: String,
    val color: String,
)

data class GoalCurrentMonth(
    val plannedAmount: BigDecimal,
    val contributedAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val progressPercentage: BigDecimal,
    val contributions: List<GoalMonthlyContribution>,
)

data class GoalMonthlyContribution(
    val goalId: UUID,
    val goalName: String,
    val amount: BigDecimal,
    val color: String,
)

data class GoalRecommendation(
    val type: GoalRecommendationType,
    val goalId: UUID,
    val goalName: String,
    val targetMonth: String,
    val currentMonthlyAmount: BigDecimal,
    val recommendedMonthlyAmount: BigDecimal,
    val differenceAmount: BigDecimal,
)
