package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.PositiveOrZero
import ru.digitalhustle.certis.enums.BudgetExpenseType
import java.math.BigDecimal
import java.util.UUID

data class SaveBudgetAllocationRq(

    val categoryId: UUID,

    val type: BudgetExpenseType,

    @field:PositiveOrZero
    @field:Digits(integer = 15, fraction = 4)
    val limit: BigDecimal,
)
