package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.model.entity.GoalTransaction
import ru.digitalhustle.certis.model.goal.GoalContributionFilter
import ru.digitalhustle.certis.model.goal.GoalContributionPage
import ru.digitalhustle.certis.model.goal.GoalMonthlyAmount
import ru.digitalhustle.certis.model.goal.NewGoalTransaction
import java.time.OffsetDateTime
import java.util.UUID

interface GoalTransactionService {

    fun getByIdForUpdate(id: UUID, userId: UUID, goalId: UUID): GoalTransaction

    fun findByIdempotencyKey(idempotencyKey: String, userId: UUID): GoalTransaction?

    fun findRefund(contributionId: UUID, userId: UUID): GoalTransaction?

    fun getPage(goalId: UUID, userId: UUID, filter: GoalContributionFilter): GoalContributionPage

    fun getNetAmounts(
        userId: UUID,
        currency: Currency,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<GoalMonthlyAmount>

    fun save(transaction: NewGoalTransaction): GoalTransaction
}
