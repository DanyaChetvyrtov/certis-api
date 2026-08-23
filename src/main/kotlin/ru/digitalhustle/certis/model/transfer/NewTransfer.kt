package ru.digitalhustle.certis.model.transfer

import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class NewTransfer(

    val userId: UUID,

    val sourceAccountId: UUID,

    val destinationAccountId: UUID,

    val currency: Currency,

    val amount: BigDecimal,

    val note: String?,

    val occurredAt: OffsetDateTime,

    val reversalOfTransferId: UUID? = null,
)
