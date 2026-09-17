package ru.digitalhustle.certis.api.dto.request

import jakarta.validation.constraints.PositiveOrZero

data class ApplyBudgetOptimizationRq(

    @field:PositiveOrZero
    val expectedVersion: Long,
)
