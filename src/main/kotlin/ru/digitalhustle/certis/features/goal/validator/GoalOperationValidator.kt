package ru.digitalhustle.certis.features.goal.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.account.api.AccountSnapshot
import ru.digitalhustle.certis.features.account.exceptions.AccountClosedException
import ru.digitalhustle.certis.features.goal.enums.GoalStatus
import ru.digitalhustle.certis.features.goal.enums.GoalTransactionType
import ru.digitalhustle.certis.features.goal.exceptions.InvalidGoalException
import ru.digitalhustle.certis.features.goal.model.CreateGoalContributionData
import ru.digitalhustle.certis.features.goal.model.Goal
import ru.digitalhustle.certis.features.goal.model.GoalTransaction
import java.math.BigDecimal
import java.time.YearMonth

@Component
class GoalOperationValidator {

    fun validateTargetMonth(currentMonth: YearMonth, targetMonth: YearMonth) {
        if (targetMonth.isBefore(currentMonth)) {
            throw InvalidGoalException(ErrorMessages.GOAL_TARGET_MONTH_PAST)
        }
    }

    fun validateInitialAmount(
        initialAmount: BigDecimal,
        targetAmount: BigDecimal,
    ) {
        if (initialAmount >= targetAmount) {
            throw InvalidGoalException(ErrorMessages.GOAL_INITIAL_AMOUNT_INVALID)
        }
    }

    fun validateAccount(
        account: AccountSnapshot,
        goalCurrency: Currency,
    ) {
        if (account.closedAt != null) {
            throw AccountClosedException(ErrorMessages.GOAL_ACCOUNT_CLOSED)
        }
        if (account.currency != goalCurrency) {
            throw InvalidGoalException(ErrorMessages.GOAL_ACCOUNT_CURRENCY_MISMATCH)
        }
    }

    fun validateMutable(goal: Goal) {
        if (goal.status == GoalStatus.ACHIEVED || goal.status == GoalStatus.CANCELLED) {
            throw InvalidGoalException(ErrorMessages.GOAL_TERMINAL)
        }
    }

    fun validateActive(goal: Goal) {
        if (goal.status != GoalStatus.ACTIVE) {
            throw InvalidGoalException(ErrorMessages.GOAL_NOT_ACTIVE)
        }
    }

    fun validateRequestedStatus(status: GoalStatus?) {
        if (status != null && status != GoalStatus.ACTIVE && status != GoalStatus.PAUSED) {
            throw InvalidGoalException(ErrorMessages.GOAL_TERMINAL)
        }
    }

    fun validateRefund(contribution: GoalTransaction) {
        if (contribution.type != GoalTransactionType.CONTRIBUTION || contribution.reversalOfGoalTransactionId != null) {
            throw InvalidGoalException(ErrorMessages.GOAL_REVERSAL_INVALID)
        }
    }

    fun validateIdempotentContribution(
        existing: GoalTransaction,
        data: CreateGoalContributionData,
    ) {
        if (
            existing.type != GoalTransactionType.CONTRIBUTION ||
            existing.goalId != data.goalId ||
            existing.accountId != data.accountId ||
            existing.amount.compareTo(data.amount) != 0
        ) {
            throw InvalidGoalException(ErrorMessages.GOAL_IDEMPOTENCY_CONFLICT)
        }
    }

    fun normalizeIdempotencyKey(idempotencyKey: String?): String? =
        idempotencyKey?.trim()?.takeIf(String::isNotEmpty)
}
