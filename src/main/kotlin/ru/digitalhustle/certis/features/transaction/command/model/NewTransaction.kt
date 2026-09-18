package ru.digitalhustle.certis.features.transaction.command.model

import ru.digitalhustle.certis.features.transaction.enums.TransactionType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class NewTransaction(

    val userId: UUID,

    val accountId: UUID,

    val type: TransactionType,

    val amount: BigDecimal,

    val categoryId: UUID?,

    val merchant: String?,

    val note: String?,

    val occurredAt: OffsetDateTime,

    val transferId: UUID? = null,
)
