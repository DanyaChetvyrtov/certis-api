package ru.digitalhustle.certis.features.goal.command.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.features.goal.enums.GoalContributionPlanType
import ru.digitalhustle.certis.features.goal.enums.GoalStatus
import ru.digitalhustle.certis.features.goal.exceptions.InvalidGoalException
import ru.digitalhustle.certis.features.goal.model.CreateGoalData
import ru.digitalhustle.certis.features.goal.model.Goal
import ru.digitalhustle.certis.features.goal.model.GoalContributionPlanData
import ru.digitalhustle.certis.features.goal.model.NewGoal
import ru.digitalhustle.certis.features.goal.model.UpdateGoalData
import ru.digitalhustle.certis.features.goal.util.GoalCalculator
import ru.digitalhustle.certis.features.goal.validator.GoalOperationValidator
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.math.BigDecimal
import java.time.YearMonth

@Component
class GoalPlanner(
    private val goalCalculator: GoalCalculator,
    private val goalOperationValidator: GoalOperationValidator,
    private val applicationClock: ApplicationClock,
) {

    fun create(data: CreateGoalData): NewGoal {
        goalOperationValidator.validateTargetMonth(applicationClock.currentMonth(), data.targetMonth)

        val initialAmount = data.initialContribution?.amount ?: BigDecimal.ZERO
        goalOperationValidator.validateInitialAmount(initialAmount, data.targetAmount)

        val recommendedMonthlyAmount = goalCalculator.recommended(
            goalCalculator.remaining(data.targetAmount, initialAmount),
            goalCalculator.monthsUntil(data.targetMonth),
        )
        val selectedMonthlyAmount = goalCalculator.selectedMonthlyAmount(
            data.contributionPlan,
            recommendedMonthlyAmount,
        )

        return NewGoal(
            userId = data.userId,
            name = data.name,
            targetAmount = data.targetAmount,
            currency = data.currency,
            targetMonth = data.targetMonth,
            contributionPlanType = data.contributionPlan.type,
            monthlyContributionAmount = selectedMonthlyAmount,
            icon = data.icon,
            color = data.color,
        )
    }

    fun update(
        current: Goal,
        data: UpdateGoalData,
        savedAmount: BigDecimal,
    ): Goal {
        goalOperationValidator.validateMutable(current)
        data.targetMonth?.let { targetMonth ->
            goalOperationValidator.validateTargetMonth(applicationClock.currentMonth(), targetMonth)
        }
        goalOperationValidator.validateRequestedStatus(data.status)

        val targetAmount = data.targetAmount ?: current.targetAmount
        val remainingAmount = goalCalculator.remaining(targetAmount, savedAmount)
        val targetMonth = data.targetMonth
            ?: current.deadline?.let(YearMonth::from)
            ?: throw InvalidGoalException(ErrorMessages.GOAL_TARGET_MONTH_PAST)
        val recommendedMonthlyAmount = goalCalculator.recommended(
            remainingAmount,
            goalCalculator.monthsUntil(targetMonth),
        )
        val planType = data.contributionPlan?.type ?: current.contributionPlanType
        val monthlyContributionAmount = resolveMonthlyContributionAmount(
            contributionPlan = data.contributionPlan,
            current = current,
            remainingAmount = remainingAmount,
            recommendedMonthlyAmount = recommendedMonthlyAmount,
            planType = planType,
        )
        val now = applicationClock.now()
        val achieved = remainingAmount.signum() == 0

        return current.copy(
            name = data.name?.trim() ?: current.name,
            targetAmount = targetAmount,
            deadline = targetMonth.atEndOfMonth(),
            contributionPlanType = planType,
            monthlyContributionAmount = monthlyContributionAmount,
            icon = data.icon?.trim() ?: current.icon,
            color = data.color?.uppercase() ?: current.color,
            status = if (achieved) GoalStatus.ACHIEVED else data.status ?: current.status,
            achievedAt = if (achieved) now else null,
            archivedAt = if (achieved) now else null,
        )
    }

    private fun resolveMonthlyContributionAmount(
        contributionPlan: GoalContributionPlanData?,
        current: Goal,
        remainingAmount: BigDecimal,
        recommendedMonthlyAmount: BigDecimal,
        planType: GoalContributionPlanType,
    ): BigDecimal =
        when {
            remainingAmount.signum() == 0 -> current.monthlyContributionAmount
            contributionPlan != null -> goalCalculator.selectedMonthlyAmount(
                contributionPlan,
                recommendedMonthlyAmount,
            )
            planType == GoalContributionPlanType.RECOMMENDED -> recommendedMonthlyAmount
            else -> current.monthlyContributionAmount
        }
}
