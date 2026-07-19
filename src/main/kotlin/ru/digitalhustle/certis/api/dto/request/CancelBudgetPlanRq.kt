package ru.digitalhustle.certis.api.dto.request

import jakarta.validation.constraints.PositiveOrZero

data class CancelBudgetPlanRq(

    @field:PositiveOrZero
    val expectedVersion: Long,
)
