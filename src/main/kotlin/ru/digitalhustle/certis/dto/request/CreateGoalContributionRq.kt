package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class CreateGoalContributionRq(

    val accountId: UUID,

    @field:Positive
    @field:Digits(integer = 15, fraction = 4)
    val amount: BigDecimal,

    val contributedAt: OffsetDateTime? = null,

    @field:Size(max = 500)
    val note: String? = null,
)
