package ru.digitalhustle.certis.features.goal.command.service

import ru.digitalhustle.certis.features.goal.model.GoalTransaction
import ru.digitalhustle.certis.features.goal.model.NewGoalTransaction
import java.math.BigDecimal
import java.util.UUID

interface GoalTransactionService {

    fun getByIdForUpdate(id: UUID, userId: UUID, goalId: UUID): GoalTransaction

    fun findByIdempotencyKey(idempotencyKey: String, userId: UUID): GoalTransaction?

    fun findRefund(contributionId: UUID, userId: UUID): GoalTransaction?

    fun save(transaction: NewGoalTransaction): GoalTransaction

    fun getSavedAmount(goalId: UUID, userId: UUID): BigDecimal
}
