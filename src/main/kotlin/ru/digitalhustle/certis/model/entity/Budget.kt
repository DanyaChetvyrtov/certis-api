package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Budget(

    val id: UUID,

    val userId: UUID,

    val periodStart: LocalDate,

    val periodEnd: LocalDate,

    val totalBudget: BigDecimal,

    val currency: Currency,

    val createdAt: OffsetDateTime,
)
