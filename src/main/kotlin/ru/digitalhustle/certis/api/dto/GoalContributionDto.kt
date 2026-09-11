package ru.digitalhustle.certis.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.goal.enums.GoalTransactionType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GoalContributionDto(

    val id: UUID,

    val goalId: UUID,

    val accountId: UUID,

    val reversalOfContributionId: UUID?,

    val currency: Currency,

    val type: GoalTransactionType,

    val amount: BigDecimal,

    val note: String?,

    val contributedAt: OffsetDateTime,

    val createdAt: OffsetDateTime,
)
