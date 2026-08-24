package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalTransactionType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class GoalTransaction(

    val id: UUID,

    val userId: UUID,

    val goalId: UUID,

    val accountId: UUID,

    val currency: Currency,

    val type: GoalTransactionType,

    val amount: BigDecimal,

    val date: OffsetDateTime,

    val createdAt: OffsetDateTime,
)
