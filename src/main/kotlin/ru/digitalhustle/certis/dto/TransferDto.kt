package ru.digitalhustle.certis.dto

import com.fasterxml.jackson.annotation.JsonInclude
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TransferDto(

    val id: UUID,

    val sourceAccountId: UUID,

    val destinationAccountId: UUID,

    val reversalOfTransferId: UUID?,

    val currency: Currency,

    val amount: BigDecimal,

    val note: String?,

    val occurredAt: OffsetDateTime,

    val createdAt: OffsetDateTime,
)
