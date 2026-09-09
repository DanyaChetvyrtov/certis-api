package ru.digitalhustle.certis.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalPaceStatus
import ru.digitalhustle.certis.enums.GoalRecommendationType
import java.math.BigDecimal
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GoalOverviewRs(

    val month: String,

    val currency: Currency,

    val summary: GoalOverviewSummaryRs,

    val nearestTarget: NearestGoalTargetRs?,

    val currentMonth: GoalCurrentMonthRs,

    val recommendation: GoalRecommendationRs?,
)

data class GoalOverviewSummaryRs(

    val totalSavedAmount: BigDecimal,

    val contributedThisMonthAmount: BigDecimal,

    val plannedMonthlyAmount: BigDecimal,

    val monthlyPlanCompletionPercentage: BigDecimal,

    val activeGoalCount: Int,

    val healthyGoalCount: Int,

    val attentionGoalCount: Int,
)

data class NearestGoalTargetRs(

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

data class GoalCurrentMonthRs(

    val plannedAmount: BigDecimal,

    val contributedAmount: BigDecimal,

    val remainingAmount: BigDecimal,

    val progressPercentage: BigDecimal,

    val contributions: List<GoalMonthlyContributionRs>,
)

data class GoalMonthlyContributionRs(

    val goalId: UUID,

    val goalName: String,

    val amount: BigDecimal,

    val color: String,
)

data class GoalRecommendationRs(

    val type: GoalRecommendationType,

    val goalId: UUID,

    val goalName: String,

    val targetMonth: String,

    val currentMonthlyAmount: BigDecimal,

    val recommendedMonthlyAmount: BigDecimal,

    val differenceAmount: BigDecimal,
)
