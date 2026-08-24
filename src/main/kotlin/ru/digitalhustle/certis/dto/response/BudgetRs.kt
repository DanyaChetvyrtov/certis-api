package ru.digitalhustle.certis.dto.response

import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetRs(

    val id: UUID,

    val month: String,

    val currency: Currency,

    val monthlyIncome: BigDecimal,

    val savingsTarget: BigDecimal,

    val allocations: List<BudgetAllocationRs>,

    val createdAt: OffsetDateTime,

    val updatedAt: OffsetDateTime,
)
