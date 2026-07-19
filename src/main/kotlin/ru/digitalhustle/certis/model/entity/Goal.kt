package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.enums.GoalStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Goal(

    val id: UUID,

    val userId: UUID,

    val name: String,

    val targetAmount: BigDecimal,

    val currency: Currency,

    val deadline: LocalDate?,

    val status: GoalStatus,

    val achievedAt: OffsetDateTime?,

    val archivedAt: OffsetDateTime?,
)
