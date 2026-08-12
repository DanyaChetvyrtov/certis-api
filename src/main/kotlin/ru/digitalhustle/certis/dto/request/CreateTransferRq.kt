package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class CreateTransferRq(

    val sourceAccountId: UUID,

    val destinationAccountId: UUID,

    @field:Positive
    @field:Digits(integer = 15, fraction = 4)
    val amount: BigDecimal,

    val note: String?,

    val occurredAt: OffsetDateTime,
)
