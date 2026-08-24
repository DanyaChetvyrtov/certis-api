package ru.digitalhustle.certis.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class SaveBudgetRq(

    @field:PositiveOrZero
    @field:Digits(integer = 15, fraction = 4)
    val monthlyIncome: BigDecimal,

    @field:PositiveOrZero
    @field:Digits(integer = 15, fraction = 4)
    val savingsTarget: BigDecimal,

    @field:Valid
    @field:Size(max = 500)
    val allocations: List<SaveBudgetAllocationRq>,
)
