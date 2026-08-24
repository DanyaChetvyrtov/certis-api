package ru.digitalhustle.certis.model.transfer

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class CreateTransferData(

    val userId: UUID,

    val sourceAccountId: UUID,

    val destinationAccountId: UUID,

    val amount: BigDecimal,

    val note: String?,

    val occurredAt: OffsetDateTime,
)
