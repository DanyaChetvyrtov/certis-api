package ru.digitalhustle.certis.model.goal

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalContributionPlanType
import ru.digitalhustle.certis.enums.GoalContributionSort
import ru.digitalhustle.certis.enums.GoalSort
import ru.digitalhustle.certis.enums.GoalStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

data class GoalFilter(
    val currency: Currency?,
    val status: GoalStatus,
    val sort: GoalSort,
    val page: Int,
    val size: Int,
)

data class GoalOverviewFilter(
    val month: YearMonth?,
    val currency: Currency?,
)

data class GoalContributionPlanData(
    val type: GoalContributionPlanType,
    val monthlyAmount: BigDecimal?,
)

data class InitialGoalContributionData(
    val accountId: UUID,
    val amount: BigDecimal,
    val note: String?,
    val contributedAt: OffsetDateTime?,
)

data class CreateGoalData(
    val userId: UUID,
    val name: String,
    val targetAmount: BigDecimal,
    val currency: Currency,
    val targetMonth: YearMonth,
    val contributionPlan: GoalContributionPlanData,
    val initialContribution: InitialGoalContributionData?,
    val icon: String,
    val color: String,
)

data class GoalPlanPreviewData(
    val targetAmount: BigDecimal,
    val initialAmount: BigDecimal,
    val currency: Currency,
    val targetMonth: YearMonth,
    val contributionPlan: GoalContributionPlanData,
)

data class UpdateGoalData(
    val id: UUID,
    val userId: UUID,
    val name: String?,
    val targetAmount: BigDecimal?,
    val targetMonth: YearMonth?,
    val contributionPlan: GoalContributionPlanData?,
    val icon: String?,
    val color: String?,
    val status: GoalStatus?,
)

data class CreateGoalContributionData(
    val goalId: UUID,
    val userId: UUID,
    val accountId: UUID,
    val amount: BigDecimal,
    val contributedAt: OffsetDateTime?,
    val note: String?,
    val idempotencyKey: String?,
)

data class GoalContributionFilter(
    val sort: GoalContributionSort,
    val page: Int,
    val size: Int,
)

data class GoalMonthlyAmount(
    val goalId: UUID,
    val amount: BigDecimal,
)

data class NewGoal(
    val userId: UUID,
    val name: String,
    val targetAmount: BigDecimal,
    val currency: Currency,
    val targetMonth: YearMonth,
    val contributionPlanType: GoalContributionPlanType,
    val monthlyContributionAmount: BigDecimal,
    val icon: String,
    val color: String,
)

data class NewGoalTransaction(
    val userId: UUID,
    val goalId: UUID,
    val accountId: UUID,
    val reversalOfGoalTransactionId: UUID? = null,
    val currency: Currency,
    val type: ru.digitalhustle.certis.enums.GoalTransactionType,
    val amount: BigDecimal,
    val idempotencyKey: String? = null,
    val note: String? = null,
    val date: OffsetDateTime?,
)
