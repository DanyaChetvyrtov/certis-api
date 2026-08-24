package ru.digitalhustle.certis.model.entity

import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.CategoryType
import java.math.BigDecimal
import java.util.UUID

data class BudgetCategory(

    val id: UUID,

    val userId: UUID,

    val budgetId: UUID,

    val categoryId: UUID,

    val categoryType: CategoryType,

    val limitAmount: BigDecimal,

    val expenseType: BudgetExpenseType,
)
