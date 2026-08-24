package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class Budget(

    val id: UUID,

    val userId: UUID,

    val budgetMonth: LocalDate,

    val plannedIncome: BigDecimal,

    val savingsTarget: BigDecimal,

    val currency: Currency,

    val createdAt: OffsetDateTime,

    val updatedAt: OffsetDateTime,
)
