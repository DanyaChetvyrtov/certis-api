package ru.digitalhustle.certis.api.dto.request

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

data class GenerateBudgetOptimizationRq(

    @field:PositiveOrZero
    val expectedVersion: Long,

    @field:Positive
    val forecastRevision: Int,

    @field:Positive
    val constraintsRevision: Int,

    @field:PositiveOrZero
    @field:Digits(integer = 15, fraction = 4)
    val targetSavingsAmount: BigDecimal,
)
