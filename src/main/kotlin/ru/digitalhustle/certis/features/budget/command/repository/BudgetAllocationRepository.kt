package ru.digitalhustle.certis.features.budget.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.model.BudgetCategory
import java.util.UUID

@Repository
class BudgetAllocationRepository(
    private val dsl: DSLContext,
) {

    fun insertAllocations(allocations: Collection<BudgetCategory>) {
        if (allocations.isEmpty()) {
            return
        }

        dsl.batchInsert(
            allocations.map { allocation ->
                dsl.newRecord(Tables.BUDGET_CATEGORIES, allocation)
            },
        ).execute()
    }

    fun deleteAllocations(budgetId: UUID) {
        dsl.deleteFrom(Tables.BUDGET_CATEGORIES)
            .where(Tables.BUDGET_CATEGORIES.BUDGET_ID.eq(budgetId))
            .execute()
    }
}
