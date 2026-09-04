package ru.digitalhustle.certis.dto

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class UncategorizedTransactionDto(

    val id: UUID,

    val merchant: String?,

    val note: String?,

    val amount: BigDecimal,

    val occurredAt: OffsetDateTime,

    val account: AccountShortInfoDto,
)
