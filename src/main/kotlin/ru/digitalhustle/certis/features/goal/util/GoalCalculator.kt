package ru.digitalhustle.certis.features.goal.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.MoneyConstants
import ru.digitalhustle.certis.features.goal.enums.GoalContributionPlanType
import ru.digitalhustle.certis.features.goal.enums.GoalPaceStatus
import ru.digitalhustle.certis.features.goal.exceptions.InvalidGoalException
import ru.digitalhustle.certis.features.goal.model.GoalContributionPlanData
import ru.digitalhustle.certis.features.goal.model.GoalContributionPlanView
import ru.digitalhustle.certis.features.goal.model.GoalPlanPreview
import ru.digitalhustle.certis.features.goal.model.GoalPlanPreviewData
import ru.digitalhustle.certis.features.goal.model.GoalProgress
import ru.digitalhustle.certis.features.goal.model.GoalView
import ru.digitalhustle.certis.features.goal.model.GoalWithBalance
import ru.digitalhustle.certis.features.goal.validator.GoalOperationValidator
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.max

@Component
class GoalCalculator(
    private val applicationClock: ApplicationClock,
    private val goalOperationValidator: GoalOperationValidator,
) {

    fun preview(data: GoalPlanPreviewData): GoalPlanPreview {
        goalOperationValidator.validateTargetMonth(applicationClock.currentMonth(), data.targetMonth)
        goalOperationValidator.validateInitialAmount(data.initialAmount, data.targetAmount)

        val remainingAmount = remaining(data.targetAmount, data.initialAmount)
        val contributionMonths = monthsUntil(data.targetMonth)
        val recommendedMonthlyAmount = recommended(remainingAmount, contributionMonths)
        val selectedMonthlyAmount = selectedMonthlyAmount(data.contributionPlan, recommendedMonthlyAmount)
        val projectedCompletionMonth = projectedCompletionMonth(remainingAmount, selectedMonthlyAmount)

        return GoalPlanPreview(
            currency = data.currency,
            targetAmount = money(data.targetAmount),
            initialAmount = money(data.initialAmount),
            remainingAmount = remainingAmount,
            progressPercentage = progress(data.initialAmount, data.targetAmount),
            targetMonth = data.targetMonth.toString(),
            contributionMonths = contributionMonths,
            recommendedMonthlyAmount = recommendedMonthlyAmount,
            selectedMonthlyAmount = selectedMonthlyAmount,
            projectedCompletionMonth = projectedCompletionMonth?.toString(),
            paceStatus = pace(projectedCompletionMonth, data.targetMonth),
        )
    }

    fun toView(source: GoalWithBalance): GoalView {
        val goal = source.goal
        val savedAmount = money(source.savedAmount)
        val remainingAmount = remaining(goal.targetAmount, savedAmount)
        val targetMonth = goal.deadline?.let(YearMonth::from)
        val contributionMonths = targetMonth?.let(::monthsUntil) ?: 1
        val recommendedMonthlyAmount = recommended(remainingAmount, contributionMonths)
        val projectedCompletionMonth = projectedCompletionMonth(
            remainingAmount,
            goal.monthlyContributionAmount,
        )

        return GoalView(
            id = goal.id,
            name = goal.name,
            currency = goal.currency,
            targetAmount = money(goal.targetAmount),
            savedAmount = savedAmount,
            remainingAmount = remainingAmount,
            progressPercentage = progress(savedAmount, goal.targetAmount),
            targetMonth = targetMonth?.toString(),
            monthsRemaining = targetMonth?.let(::monthsUntil),
            contributionPlan = GoalContributionPlanView(
                type = goal.contributionPlanType,
                monthlyAmount = money(goal.monthlyContributionAmount),
                recommendedMonthlyAmount = recommendedMonthlyAmount,
            ),
            status = goal.status,
            paceStatus = pace(projectedCompletionMonth, targetMonth),
            projectedCompletionMonth = projectedCompletionMonth?.toString(),
            icon = goal.icon,
            color = goal.color,
            createdAt = goal.createdAt,
            updatedAt = goal.updatedAt,
            achievedAt = goal.achievedAt,
            archivedAt = goal.archivedAt,
        )
    }

    fun toProgress(source: GoalWithBalance): GoalProgress {
        val view = toView(source)

        return GoalProgress(
            id = view.id,
            savedAmount = view.savedAmount,
            remainingAmount = view.remainingAmount,
            progressPercentage = view.progressPercentage,
            status = view.status,
            paceStatus = view.paceStatus,
        )
    }

    fun selectedMonthlyAmount(
        plan: GoalContributionPlanData,
        recommendedMonthlyAmount: BigDecimal,
    ): BigDecimal =
        when (plan.type) {
            GoalContributionPlanType.RECOMMENDED -> {
                if (plan.monthlyAmount != null) {
                    throw InvalidGoalException(ErrorMessages.GOAL_PLAN_INVALID)
                }
                recommendedMonthlyAmount
            }

            GoalContributionPlanType.CUSTOM ->
                plan.monthlyAmount ?: throw InvalidGoalException(ErrorMessages.GOAL_PLAN_INVALID)
        }

    fun recommended(
        remainingAmount: BigDecimal,
        contributionMonths: Int,
    ): BigDecimal =
        if (remainingAmount.signum() == 0) {
            BigDecimal.ZERO.setScale(MoneyConstants.MONEY_SCALE)
        } else {
            remainingAmount.divide(
                contributionMonths.toBigDecimal(),
                MoneyConstants.MONEY_SCALE,
                RoundingMode.CEILING,
            )
        }

    fun progress(
        savedAmount: BigDecimal,
        targetAmount: BigDecimal,
    ): BigDecimal =
        savedAmount
            .divide(targetAmount, MoneyConstants.PERCENTAGE_SCALE + 2, RoundingMode.HALF_UP)
            .multiply(MoneyConstants.PERCENTAGE_MULTIPLIER)
            .min(MoneyConstants.PERCENTAGE_MULTIPLIER)
            .max(BigDecimal.ZERO)
            .setScale(MoneyConstants.PERCENTAGE_SCALE, RoundingMode.HALF_UP)

    fun remaining(
        targetAmount: BigDecimal,
        savedAmount: BigDecimal,
    ): BigDecimal = money((targetAmount - savedAmount).max(BigDecimal.ZERO))

    fun monthsUntil(targetMonth: YearMonth): Int {
        val currentMonth = applicationClock.currentMonth()
        return max(1, ChronoUnit.MONTHS.between(currentMonth, targetMonth).toInt() + 1)
    }

    fun money(amount: BigDecimal): BigDecimal = amount.setScale(MoneyConstants.MONEY_SCALE, RoundingMode.HALF_UP)

    private fun projectedCompletionMonth(
        remainingAmount: BigDecimal,
        monthlyAmount: BigDecimal,
    ): YearMonth? {
        if (remainingAmount.signum() == 0) {
            return applicationClock.currentMonth()
        }
        if (monthlyAmount.signum() <= 0) {
            return null
        }

        val periods = remainingAmount.divide(monthlyAmount, 0, RoundingMode.CEILING).longValueExact()
        return applicationClock.currentMonth().plusMonths(periods - 1)
    }

    private fun pace(
        projectedCompletionMonth: YearMonth?,
        targetMonth: YearMonth?,
    ): GoalPaceStatus =
        when {
            projectedCompletionMonth == null || targetMonth == null -> GoalPaceStatus.ON_TRACK
            projectedCompletionMonth.isBefore(targetMonth) -> GoalPaceStatus.AHEAD
            projectedCompletionMonth == targetMonth -> GoalPaceStatus.ON_TRACK
            else -> GoalPaceStatus.ADJUST_PLAN
        }
}
