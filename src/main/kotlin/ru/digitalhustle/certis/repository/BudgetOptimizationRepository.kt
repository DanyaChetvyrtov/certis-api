package ru.digitalhustle.certis.repository

import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.BudgetOptimizationStatus
import ru.digitalhustle.certis.model.entity.BudgetOptimization
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class BudgetOptimizationRepository(
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

    fun findByIdAndUserIdAndMonthForUpdate(
        id: UUID,
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetOptimization? =
        selectByUserIdAndMonth(userId, budgetMonth)
            .and(Tables.BUDGET_OPTIMIZATIONS.ID.eq(id))
            .forUpdate()
            .fetchOne(::toEntity)

    fun insert(optimization: BudgetOptimization): BudgetOptimization =
        dsl.insertInto(Tables.BUDGET_OPTIMIZATIONS)
            .set(Tables.BUDGET_OPTIMIZATIONS.ID, optimization.id)
            .set(Tables.BUDGET_OPTIMIZATIONS.USER_ID, optimization.userId)
            .set(Tables.BUDGET_OPTIMIZATIONS.BUDGET_ID, optimization.budgetId)
            .set(Tables.BUDGET_OPTIMIZATIONS.SNAPSHOT_SCHEMA_VERSION, optimization.snapshotSchemaVersion)
            .set(Tables.BUDGET_OPTIMIZATIONS.ALGORITHM_VERSION, optimization.algorithmVersion)
            .set(Tables.BUDGET_OPTIMIZATIONS.STATUS, optimization.status.name)
            .set(
                Tables.BUDGET_OPTIMIZATIONS.INPUT_SNAPSHOT,
                JSONB.valueOf(objectMapper.writeValueAsString(optimization.inputSnapshot)),
            )
            .set(
                Tables.BUDGET_OPTIMIZATIONS.RESULT_SNAPSHOT,
                JSONB.valueOf(objectMapper.writeValueAsString(optimization.resultSnapshot)),
            )
            .set(Tables.BUDGET_OPTIMIZATIONS.SAVINGS_BEFORE, optimization.savingsBefore)
            .set(Tables.BUDGET_OPTIMIZATIONS.SAVINGS_AFTER, optimization.savingsAfter)
            .set(Tables.BUDGET_OPTIMIZATIONS.CREATED_AT, optimization.createdAt)
            .set(Tables.BUDGET_OPTIMIZATIONS.APPLIED_AT, optimization.appliedAt)
            .returning()
            .fetchSingle(::toEntity)

    fun dismissProposedByBudgetId(
        budgetId: UUID,
        userId: UUID,
    ) {
        dsl.update(Tables.BUDGET_OPTIMIZATIONS)
            .set(Tables.BUDGET_OPTIMIZATIONS.STATUS, BudgetOptimizationStatus.DISMISSED.name)
            .where(
                Tables.BUDGET_OPTIMIZATIONS.BUDGET_ID.eq(budgetId)
                    .and(Tables.BUDGET_OPTIMIZATIONS.USER_ID.eq(userId))
                    .and(
                        Tables.BUDGET_OPTIMIZATIONS.STATUS.eq(
                            BudgetOptimizationStatus.PROPOSED.name,
                        ),
                    ),
            )
            .execute()
    }

    fun updateStatus(
        id: UUID,
        userId: UUID,
        status: BudgetOptimizationStatus,
        appliedAt: OffsetDateTime?,
    ): BudgetOptimization? =
        dsl.update(Tables.BUDGET_OPTIMIZATIONS)
            .set(Tables.BUDGET_OPTIMIZATIONS.STATUS, status.name)
            .set(Tables.BUDGET_OPTIMIZATIONS.APPLIED_AT, appliedAt)
            .where(
                Tables.BUDGET_OPTIMIZATIONS.ID.eq(id)
                    .and(Tables.BUDGET_OPTIMIZATIONS.USER_ID.eq(userId))
                    .and(
                        Tables.BUDGET_OPTIMIZATIONS.STATUS.eq(
                            BudgetOptimizationStatus.PROPOSED.name,
                        ),
                    ),
            )
            .returning()
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
