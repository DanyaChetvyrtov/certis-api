package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.GoalTransaction
import ru.digitalhustle.certis.model.goal.GoalContributionFilter
import ru.digitalhustle.certis.model.goal.GoalContributionPage
import ru.digitalhustle.certis.model.goal.GoalMonthlyAmount
import ru.digitalhustle.certis.model.goal.NewGoalTransaction
import ru.digitalhustle.certis.repository.GoalTransactionRepository
import ru.digitalhustle.certis.service.domain.GoalTransactionService
import ru.digitalhustle.certis.time.ApplicationClock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class GoalTransactionServiceImpl(
    private val goalTransactionRepository: GoalTransactionRepository,
    private val applicationClock: ApplicationClock,
) : GoalTransactionService {

    override fun getByIdForUpdate(
        id: UUID,
        userId: UUID,
        goalId: UUID,
    ): GoalTransaction =
        goalTransactionRepository.findByIdAndUserIdAndGoalIdForUpdate(id, userId, goalId)
            ?: throw NotFoundException.entity("Goal contribution")

    override fun findByIdempotencyKey(
        idempotencyKey: String,
        userId: UUID,
    ): GoalTransaction? = goalTransactionRepository.findByIdempotencyKeyAndUserId(idempotencyKey, userId)

    override fun findRefund(
        contributionId: UUID,
        userId: UUID,
    ): GoalTransaction? = goalTransactionRepository.findRefundByContributionIdAndUserId(contributionId, userId)

    override fun getPage(
        goalId: UUID,
        userId: UUID,
        filter: GoalContributionFilter,
    ): GoalContributionPage =
        GoalContributionPage(
            items = goalTransactionRepository.findPageByGoalIdAndUserId(goalId, userId, filter),
            page = filter.page,
            size = filter.size,
            totalElements = goalTransactionRepository.countByGoalIdAndUserId(goalId, userId),
        )

    override fun getNetAmounts(
        userId: UUID,
        currency: Currency,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<GoalMonthlyAmount> = goalTransactionRepository.findNetAmountsByGoalId(userId, currency, from, to)

    override fun save(transaction: NewGoalTransaction): GoalTransaction {
        val now = applicationClock.now()

        return goalTransactionRepository.insert(
            GoalTransaction(
                id = UUID.randomUUID(),
                userId = transaction.userId,
                goalId = transaction.goalId,
                accountId = transaction.accountId,
                reversalOfGoalTransactionId = transaction.reversalOfGoalTransactionId,
                currency = transaction.currency,
                type = transaction.type,
                amount = transaction.amount,
                idempotencyKey = transaction.idempotencyKey,
                note = transaction.note?.trim()?.takeIf(String::isNotEmpty),
                date = transaction.date ?: now,
                createdAt = now,
            ),
        )
    }
}
