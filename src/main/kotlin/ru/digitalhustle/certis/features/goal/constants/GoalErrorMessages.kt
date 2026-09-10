package ru.digitalhustle.certis.features.goal.constants

object GoalErrorMessages {

    const val GOAL_ACCOUNT_CLOSED = "Closed account cannot be used for a goal contribution"
    const val GOAL_ACCOUNT_CURRENCY_MISMATCH = "Goal and account currencies must match"
    const val GOAL_TERMINAL = "Achieved or cancelled goal cannot be changed"
    const val GOAL_NOT_ACTIVE = "Only an active goal can receive contributions"
    const val GOAL_PLAN_INVALID = "Custom plan requires a monthly amount; recommended plan calculates it"
    const val GOAL_TARGET_MONTH_PAST = "Target month must not be in the past"
    const val GOAL_INITIAL_AMOUNT_INVALID = "Initial contribution must be less than target amount"
    const val GOAL_REVERSAL_INVALID = "Only an unreversed contribution can be refunded"
    const val GOAL_IDEMPOTENCY_CONFLICT = "Idempotency key is already used for another contribution"
}
