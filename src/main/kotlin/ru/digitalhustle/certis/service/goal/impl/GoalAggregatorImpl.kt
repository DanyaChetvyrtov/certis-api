package ru.digitalhustle.certis.service.goal.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.GoalContributionPlanType
import ru.digitalhustle.certis.enums.GoalStatus
import ru.digitalhustle.certis.enums.GoalTransactionType
import ru.digitalhustle.certis.exception.custom.InvalidGoalException
import ru.digitalhustle.certis.model.entity.Goal
import ru.digitalhustle.certis.model.entity.GoalTransaction
import ru.digitalhustle.certis.model.goal.CreateGoalContributionData
import ru.digitalhustle.certis.model.goal.CreateGoalData
import ru.digitalhustle.certis.model.goal.GoalContributionFilter
import ru.digitalhustle.certis.model.goal.GoalContributionPage
import ru.digitalhustle.certis.model.goal.GoalContributionPlanData
import ru.digitalhustle.certis.model.goal.GoalContributionResult
import ru.digitalhustle.certis.model.goal.GoalFilter
import ru.digitalhustle.certis.model.goal.GoalOverview
import ru.digitalhustle.certis.model.goal.GoalOverviewFilter
import ru.digitalhustle.certis.model.goal.GoalPage
import ru.digitalhustle.certis.model.goal.GoalPlanPreview
import ru.digitalhustle.certis.model.goal.GoalPlanPreviewData
import ru.digitalhustle.certis.model.goal.GoalProgress
import ru.digitalhustle.certis.model.goal.GoalView
import ru.digitalhustle.certis.model.goal.GoalWithBalance
import ru.digitalhustle.certis.model.goal.NewGoal
import ru.digitalhustle.certis.model.goal.NewGoalTransaction
import ru.digitalhustle.certis.model.goal.UpdateGoalData
import ru.digitalhustle.certis.service.domain.AccountService
import ru.digitalhustle.certis.service.domain.GoalQueryService
import ru.digitalhustle.certis.service.domain.GoalService
import ru.digitalhustle.certis.service.domain.GoalTransactionService
import ru.digitalhustle.certis.service.domain.UserService
import ru.digitalhustle.certis.service.goal.GoalAggregator
import ru.digitalhustle.certis.service.goal.GoalCalculator
import ru.digitalhustle.certis.service.goal.GoalLifecycleManager
import ru.digitalhustle.certis.service.goal.GoalOperationValidator
import ru.digitalhustle.certis.service.goal.GoalOverviewFactory
import ru.digitalhustle.certis.time.ApplicationClock
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

@Service
class GoalAggregatorImpl(
    private val goalService: GoalService,
    private val goalQueryService: GoalQueryService,
    private val goalTransactionService: GoalTransactionService,
    private val accountService: AccountService,
    private val userService: UserService,
    private val goalCalculator: GoalCalculator,
    private val goalLifecycleManager: GoalLifecycleManager,
    private val goalOverviewFactory: GoalOverviewFactory,
    private val goalOperationValidator: GoalOperationValidator,
    private val applicationClock: ApplicationClock,
) : GoalAggregator {

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getPage(
        userId: UUID,
        filter: GoalFilter,
    ): GoalPage {
        val currency = filter.currency ?: userService.getUserById(userId).preferredCurrency
        return goalQueryService.getPage(userId, currency, filter)
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getOverview(
        userId: UUID,
        filter: GoalOverviewFilter,
    ): GoalOverview {
        val currency = filter.currency ?: userService.getUserById(userId).preferredCurrency
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

    @Transactional
    override fun create(data: CreateGoalData): GoalView {
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

        val goal = goalService.save(
            NewGoal(
                userId = data.userId,
                name = data.name,
                targetAmount = data.targetAmount,
                currency = data.currency,
                targetMonth = data.targetMonth,
                contributionPlanType = data.contributionPlan.type,
                monthlyContributionAmount = selectedMonthlyAmount,
                icon = data.icon,
                color = data.color,
            ),
        )

        val initialAccount = data.initialContribution?.let { initial ->
            accountService.getByIdForShare(initial.accountId, data.userId).also { account ->
                goalOperationValidator.validateAccount(account, data.currency)
            }
        }

        if (initialAccount != null) {
            val initial = data.initialContribution
            goalTransactionService.save(
                NewGoalTransaction(
                    userId = data.userId,
                    goalId = goal.id,
                    accountId = initialAccount.id,
                    currency = data.currency,
                    type = GoalTransactionType.CONTRIBUTION,
                    amount = initial.amount,
                    note = initial.note,
                    date = initial.contributedAt,
                ),
            )
        }

        return goalQueryService.getById(goal.id, data.userId)
    }

    @Transactional
    override fun update(data: UpdateGoalData): GoalView {
        val current = goalService.getByIdForUpdate(data.id, data.userId)

        goalOperationValidator.validateMutable(current)
        data.targetMonth?.let {
            goalOperationValidator.validateTargetMonth(applicationClock.currentMonth(), it)
        }
        goalOperationValidator.validateRequestedStatus(data.status)

        val currentView = goalQueryService.getById(data.id, data.userId)

        val targetAmount = data.targetAmount ?: current.targetAmount
        val remainingAmount = goalCalculator.remaining(targetAmount, currentView.savedAmount)

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
        val status = if (achieved) GoalStatus.ACHIEVED else data.status ?: current.status

        goalService.update(
            current.copy(
                name = data.name?.trim() ?: current.name,
                targetAmount = targetAmount,
                deadline = targetMonth.atEndOfMonth(),
                contributionPlanType = planType,
                monthlyContributionAmount = monthlyContributionAmount,
                icon = data.icon?.trim() ?: current.icon,
                color = data.color?.uppercase() ?: current.color,
                status = status,
                achievedAt = if (achieved) now else null,
                archivedAt = if (achieved) now else null,
            ),
        )

        return goalQueryService.getById(data.id, data.userId)
    }

    @Transactional
    override fun contribute(data: CreateGoalContributionData): GoalContributionResult {
        val goal = goalService.getByIdForUpdate(data.goalId, data.userId)
        val idempotencyKey = goalOperationValidator.normalizeIdempotencyKey(data.idempotencyKey)
        idempotencyKey?.let { key ->
            goalTransactionService.findByIdempotencyKey(key, data.userId)?.let { existing ->
                goalOperationValidator.validateIdempotentContribution(existing, data)
                return contributionResult(existing, data.goalId, data.userId)
            }
        }

        if (goal.status != GoalStatus.ACTIVE) {
            throw InvalidGoalException(ErrorMessages.GOAL_NOT_ACTIVE)
        }

        val account = accountService.getByIdForShare(data.accountId, data.userId)
        goalOperationValidator.validateAccount(account, goal.currency)
        val contribution = goalTransactionService.save(
            NewGoalTransaction(
                userId = data.userId,
                goalId = goal.id,
                accountId = account.id,
                currency = goal.currency,
                type = GoalTransactionType.CONTRIBUTION,
                amount = data.amount,
                idempotencyKey = idempotencyKey,
                note = data.note,
                date = data.contributedAt,
            ),
        )

        val goalSavedAmount = goalQueryService.getById(data.goalId, data.userId).savedAmount
        goalLifecycleManager.achieveIfFunded(goal, goalSavedAmount)

        return contributionResult(contribution, goal.id, goal.userId)
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getContributions(
        goalId: UUID,
        userId: UUID,
        filter: GoalContributionFilter,
    ): GoalContributionPage {
        goalService.getById(goalId, userId)
        return goalTransactionService.getPage(goalId, userId, filter)
    }

    @Transactional
    override fun refundContribution(
        goalId: UUID,
        contributionId: UUID,
        userId: UUID,
    ): GoalProgress {
        val goal = goalService.getByIdForUpdate(goalId, userId)
        val goalSavedAmount = goalQueryService.getById(goalId, userId).savedAmount
        val contribution = goalTransactionService.getByIdForUpdate(contributionId, userId, goalId)
        goalOperationValidator.validateRefund(contribution)

        goalTransactionService.findRefund(contributionId, userId)?.let {
            return goalCalculator.toProgress(
                GoalWithBalance(
                    goal = goalService.getById(goalId, userId),
                    savedAmount = goalSavedAmount,
                ),
            )
        }

        goalTransactionService.save(
            NewGoalTransaction(
                userId = userId,
                goalId = goalId,
                accountId = contribution.accountId,
                reversalOfGoalTransactionId = contribution.id,
                currency = contribution.currency,
                type = GoalTransactionType.REFUND,
                amount = contribution.amount,
                note = "Contribution reversal",
                date = null,
            ),
        )

        val updatedGoalSavedAmount = goalQueryService.getById(goalId, userId).savedAmount
        goalLifecycleManager.reopenIfUnderfunded(goal, updatedGoalSavedAmount)

        return goalCalculator.toProgress(
            GoalWithBalance(
                goal = goalService.getById(goalId, userId),
                savedAmount = updatedGoalSavedAmount,
            ),
        )
    }

    @Transactional
    override fun cancel(
        goalId: UUID,
        userId: UUID,
    ) {
        goalLifecycleManager.cancel(goalId, userId)
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

    private fun contributionResult(
        contribution: GoalTransaction,
        goalId: UUID,
        userId: UUID,
    ): GoalContributionResult =
        GoalContributionResult(
            contribution = contribution,
            goal = goalCalculator.toProgress(
                GoalWithBalance(
                    goal = goalService.getById(goalId, userId),
                    savedAmount = goalQueryService.getById(goalId, userId).savedAmount,
                ),
            ),
        )
}
