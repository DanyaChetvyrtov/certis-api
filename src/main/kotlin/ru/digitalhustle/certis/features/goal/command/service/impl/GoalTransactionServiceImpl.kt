package ru.digitalhustle.certis.features.goal.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.goal.command.repository.GoalTransactionRepository
import ru.digitalhustle.certis.features.goal.command.service.GoalTransactionService
import ru.digitalhustle.certis.features.goal.model.GoalTransaction
import ru.digitalhustle.certis.features.goal.model.NewGoalTransaction
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.math.BigDecimal
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

    override fun getSavedAmount(goalId: UUID, userId: UUID): BigDecimal =
        goalTransactionRepository.findSavedAmount(goalId, userId)
}
