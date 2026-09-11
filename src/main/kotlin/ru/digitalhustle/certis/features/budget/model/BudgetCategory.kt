package ru.digitalhustle.certis.features.budget.model

import ru.digitalhustle.certis.features.budget.enums.BudgetExpenseType
import ru.digitalhustle.certis.features.category.enums.CategoryType
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
