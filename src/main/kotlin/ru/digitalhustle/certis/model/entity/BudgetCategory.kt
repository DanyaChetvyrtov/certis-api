package ru.digitalhustle.certis.model.entity

import java.math.BigDecimal
import java.util.UUID

data class BudgetCategory(

    val id: UUID,

    val userId: UUID,

    val budgetId: UUID,

    val categoryId: UUID,

    val limitAmount: BigDecimal,
)
