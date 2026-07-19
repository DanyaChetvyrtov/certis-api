package ru.digitalhustle.certis.features.goal.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.account.api.AccountCommandAccess
import ru.digitalhustle.certis.features.goal.application.service.GoalContributionApplicationService
import ru.digitalhustle.certis.features.goal.application.service.GoalContributionSupport
import ru.digitalhustle.certis.features.goal.command.service.GoalService
import ru.digitalhustle.certis.features.goal.command.service.GoalTransactionService
import ru.digitalhustle.certis.features.goal.command.util.GoalLifecycleManager
import ru.digitalhustle.certis.features.goal.enums.GoalTransactionType
import ru.digitalhustle.certis.features.goal.model.CreateGoalContributionData
import ru.digitalhustle.certis.features.goal.model.Goal
import ru.digitalhustle.certis.features.goal.model.GoalContributionResult
import ru.digitalhustle.certis.features.goal.model.GoalProgress
import ru.digitalhustle.certis.features.goal.model.GoalTransaction
import ru.digitalhustle.certis.features.goal.model.GoalWithBalance
import ru.digitalhustle.certis.features.goal.model.InitialGoalContributionData
import ru.digitalhustle.certis.features.goal.model.NewGoalTransaction
import ru.digitalhustle.certis.features.goal.util.GoalCalculator
import ru.digitalhustle.certis.features.goal.validator.GoalOperationValidator
import java.math.BigDecimal
import java.util.UUID

@Service
class GoalContributionApplicationServiceImpl(
    private val goalService: GoalService,
    private val goalTransactionService: GoalTransactionService,
    private val accountAccess: AccountCommandAccess,
    private val goalCalculator: GoalCalculator,
    private val goalLifecycleManager: GoalLifecycleManager,
    private val goalOperationValidator: GoalOperationValidator,
) : GoalContributionApplicationService,
    GoalContributionSupport {

    override fun getSavedAmount(goalId: UUID, userId: UUID): BigDecimal =
        goalCalculator.money(goalTransactionService.getSavedAmount(goalId, userId))

    override fun saveInitial(
        goal: Goal,
        contribution: InitialGoalContributionData,
    ) {
        val account = accountAccess.getByIdForShare(contribution.accountId, goal.userId)
        goalOperationValidator.validateAccount(account, goal.currency)

        goalTransactionService.save(
            NewGoalTransaction(
                userId = goal.userId,
                goalId = goal.id,
                accountId = account.id,
                currency = goal.currency,
                type = GoalTransactionType.CONTRIBUTION,
                amount = contribution.amount,
                note = contribution.note,
                date = contribution.contributedAt,
            ),
        )
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

        goalOperationValidator.validateActive(goal)

        val account = accountAccess.getByIdForShare(data.accountId, data.userId)
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

        val goalSavedAmount = getSavedAmount(data.goalId, data.userId)
        goalLifecycleManager.achieveIfFunded(goal, goalSavedAmount)

        return contributionResult(contribution, goal.id, goal.userId)
    }

    @Transactional
    override fun refundContribution(
        goalId: UUID,
        contributionId: UUID,
        userId: UUID,
    ): GoalProgress {
        val goal = goalService.getByIdForUpdate(goalId, userId)
        val goalSavedAmount = getSavedAmount(goalId, userId)
        val contribution = goalTransactionService.getByIdForUpdate(contributionId, userId, goalId)
        goalOperationValidator.validateRefund(contribution)

        goalTransactionService.findRefund(contributionId, userId)?.let {
            return progress(goalId, userId, goalSavedAmount)
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

        val updatedGoalSavedAmount = getSavedAmount(goalId, userId)
        goalLifecycleManager.reopenIfUnderfunded(goal, updatedGoalSavedAmount)

        return progress(goalId, userId, updatedGoalSavedAmount)
    }

    private fun contributionResult(
        contribution: GoalTransaction,
        goalId: UUID,
        userId: UUID,
    ): GoalContributionResult =
        GoalContributionResult(
            contribution = contribution,
            goal = progress(goalId, userId, getSavedAmount(goalId, userId)),
        )

    private fun progress(
        goalId: UUID,
        userId: UUID,
        savedAmount: BigDecimal,
    ): GoalProgress =
        goalCalculator.toProgress(
            GoalWithBalance(
                goal = goalService.getById(goalId, userId),
                savedAmount = savedAmount,
            ),
        )
}
