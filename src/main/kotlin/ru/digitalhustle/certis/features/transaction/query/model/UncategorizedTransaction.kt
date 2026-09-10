package ru.digitalhustle.certis.features.transaction.query.model

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class UncategorizedTransaction(

    val id: UUID,

    val merchant: String?,

    val note: String?,

    val amount: BigDecimal,

    val occurredAt: OffsetDateTime,

    val account: TransactionAccountView,
)
