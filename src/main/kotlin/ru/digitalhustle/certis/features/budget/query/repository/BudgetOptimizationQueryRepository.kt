package ru.digitalhustle.certis.features.budget.query.repository

import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.enums.BudgetOptimizationStatus
import ru.digitalhustle.certis.features.budget.model.BudgetOptimization
import java.time.LocalDate
import java.util.UUID

@Repository
class BudgetOptimizationQueryRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun findLatestByUserIdAndMonth(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetOptimization? =
        selectByUserIdAndMonth(userId, budgetMonth)
            .orderBy(
                Tables.BUDGET_OPTIMIZATIONS.CREATED_AT.desc(),
                Tables.BUDGET_OPTIMIZATIONS.ID.desc(),
            )
            .limit(1)
            .fetchOne(::toEntity)

    private fun selectByUserIdAndMonth(
        userId: UUID,
        budgetMonth: LocalDate,
    ) =
        dsl.select(Tables.BUDGET_OPTIMIZATIONS.fields().toList())
            .from(Tables.BUDGET_OPTIMIZATIONS)
            .join(Tables.BUDGETS)
            .on(
                Tables.BUDGETS.ID.eq(Tables.BUDGET_OPTIMIZATIONS.BUDGET_ID)
                    .and(Tables.BUDGETS.USER_ID.eq(Tables.BUDGET_OPTIMIZATIONS.USER_ID)),
            )
            .where(
                Tables.BUDGET_OPTIMIZATIONS.USER_ID.eq(userId)
                    .and(Tables.BUDGETS.BUDGET_MONTH.eq(budgetMonth)),
            )

    private fun toEntity(record: Record): BudgetOptimization =
        BudgetOptimization(
            id = requireNotNull(record[Tables.BUDGET_OPTIMIZATIONS.ID]),
            userId = requireNotNull(record[Tables.BUDGET_OPTIMIZATIONS.USER_ID]),
            budgetId = requireNotNull(record[Tables.BUDGET_OPTIMIZATIONS.BUDGET_ID]),
            snapshotSchemaVersion = requireNotNull(
                record[Tables.BUDGET_OPTIMIZATIONS.SNAPSHOT_SCHEMA_VERSION],
            ),
            algorithmVersion = requireNotNull(record[Tables.BUDGET_OPTIMIZATIONS.ALGORITHM_VERSION]),
            status = BudgetOptimizationStatus.valueOf(
                requireNotNull(record[Tables.BUDGET_OPTIMIZATIONS.STATUS]),
            ),
            inputSnapshot = objectMapper.readTree(
                requireNotNull(record[Tables.BUDGET_OPTIMIZATIONS.INPUT_SNAPSHOT]).data(),
            ),
            resultSnapshot = objectMapper.readTree(
                requireNotNull(record[Tables.BUDGET_OPTIMIZATIONS.RESULT_SNAPSHOT]).data(),
            ),
            savingsBefore = requireNotNull(record[Tables.BUDGET_OPTIMIZATIONS.SAVINGS_BEFORE]),
            savingsAfter = requireNotNull(record[Tables.BUDGET_OPTIMIZATIONS.SAVINGS_AFTER]),
            createdAt = requireNotNull(record[Tables.BUDGET_OPTIMIZATIONS.CREATED_AT]),
            appliedAt = record[Tables.BUDGET_OPTIMIZATIONS.APPLIED_AT],
        )
}
