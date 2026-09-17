package ru.digitalhustle.certis.features.budget.query.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationType
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetExpenseType
import ru.digitalhustle.certis.features.budget.enums.BudgetFeasibilityStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetFundingLevel
import ru.digitalhustle.certis.features.budget.enums.BudgetPriority
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryConstraint
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryFundingLevel
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintBaseline
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.model.BudgetForecastBaselineAllocation
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.model.BudgetPlanFeasibility
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningViolation
import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.math.BigDecimal
import java.util.UUID

@Repository
@Suppress("LargeClass")
class BudgetConstraintQueryRepository(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) {

    fun findLatestByPlanIdAndUserId(planId: UUID, userId: UUID): BudgetConstraintSet? {
        val record = dsl.select(
            *Tables.BUDGET_CONSTRAINT_REVISIONS.fields(),
            Tables.BUDGET_FORECAST_REVISIONS.REVISION,
            Tables.BUDGET_FORECAST_REVISIONS.FORECAST_INCOME,
            Tables.BUDGET_PLANS.VERSION,
        )
            .from(Tables.BUDGET_CONSTRAINT_REVISIONS)
            .join(Tables.BUDGET_FORECAST_REVISIONS)
            .on(
                Tables.BUDGET_FORECAST_REVISIONS.ID.eq(
                    Tables.BUDGET_CONSTRAINT_REVISIONS.FORECAST_REVISION_ID,
                ),
            )
            .join(Tables.BUDGET_PLANS)
            .on(
                Tables.BUDGET_PLANS.ID.eq(Tables.BUDGET_CONSTRAINT_REVISIONS.PLAN_ID)
                    .and(Tables.BUDGET_PLANS.USER_ID.eq(Tables.BUDGET_CONSTRAINT_REVISIONS.USER_ID)),
            )
            .where(
                Tables.BUDGET_CONSTRAINT_REVISIONS.PLAN_ID.eq(planId)
                    .and(Tables.BUDGET_CONSTRAINT_REVISIONS.USER_ID.eq(userId)),
            )
            .orderBy(Tables.BUDGET_CONSTRAINT_REVISIONS.REVISION.desc())
            .limit(1)
            .fetchOne() ?: return null
        return toConstraintSet(record)
    }

    fun findBaseline(budgetId: UUID?, userId: UUID): BudgetConstraintBaseline {
        if (budgetId == null) return BudgetConstraintBaseline(BigDecimal.ZERO, emptyList())
        val savingsFloor = dsl.select(Tables.BUDGETS.SAVINGS_TARGET)
            .from(Tables.BUDGETS)
            .where(Tables.BUDGETS.ID.eq(budgetId).and(Tables.BUDGETS.USER_ID.eq(userId)))
            .fetchOne(Tables.BUDGETS.SAVINGS_TARGET) ?: BigDecimal.ZERO
        return BudgetConstraintBaseline(
            savingsFloorAmount = savingsFloor,
            allocations = findBaselineAllocations(budgetId, userId),
        )
    }

    private fun findBaselineAllocations(budgetId: UUID, userId: UUID): List<BudgetForecastBaselineAllocation> =
        dsl.select(
            Tables.BUDGET_CATEGORIES.CATEGORY_ID,
            Tables.BUDGET_CATEGORIES.LIMIT_AMOUNT,
            Tables.BUDGET_CATEGORIES.EXPENSE_TYPE,
            Tables.CATEGORIES.TYPE,
            Tables.CATEGORIES.NAME,
            Tables.CATEGORIES.ICON,
            Tables.CATEGORIES.COLOR,
        )
            .from(Tables.BUDGET_CATEGORIES)
            .join(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.BUDGET_CATEGORIES.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.BUDGET_CATEGORIES.USER_ID)),
            )
            .where(
                Tables.BUDGET_CATEGORIES.BUDGET_ID.eq(budgetId)
                    .and(Tables.BUDGET_CATEGORIES.USER_ID.eq(userId)),
            )
            .fetch { record ->
                BudgetForecastBaselineAllocation(
                    category = BudgetForecastCategory(
                        id = requireNotNull(record[Tables.BUDGET_CATEGORIES.CATEGORY_ID]),
                        type = CategoryType.valueOf(requireNotNull(record[Tables.CATEGORIES.TYPE])),
                        name = requireNotNull(record[Tables.CATEGORIES.NAME]),
                        icon = requireNotNull(record[Tables.CATEGORIES.ICON]),
                        color = requireNotNull(record[Tables.CATEGORIES.COLOR]),
                    ),
                    limitAmount = requireNotNull(record[Tables.BUDGET_CATEGORIES.LIMIT_AMOUNT]),
                    fixed = BudgetExpenseType.valueOf(
                        requireNotNull(record[Tables.BUDGET_CATEGORIES.EXPENSE_TYPE]),
                    ) == BudgetExpenseType.FIXED,
                )
            }

    private fun toConstraintSet(record: Record): BudgetConstraintSet {
        val constraintId = requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.ID])
        val forecastIncome = requireNotNull(record[Tables.BUDGET_FORECAST_REVISIONS.FORECAST_INCOME])
        val userId = requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.USER_ID])
        val categories = findCategories(constraintId, userId)
        val savingsFloor = requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.SAVINGS_FLOOR_AMOUNT])
        val requiredAmount = requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.REQUIRED_AMOUNT])
        return BudgetConstraintSet(
            id = constraintId,
            userId = userId,
            planId = requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.PLAN_ID]),
            forecastRevisionId = requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.FORECAST_REVISION_ID]),
            basedOnForecastRevision = requireNotNull(record[Tables.BUDGET_FORECAST_REVISIONS.REVISION]),
            revision = requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.REVISION]),
            status = BudgetConstraintStatus.CONFIRMED,
            savingsFloorAmount = savingsFloor,
            categories = categories,
            feasibility = BudgetPlanFeasibility(
                status = BudgetFeasibilityStatus.valueOf(
                    requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.FEASIBILITY_STATUS]),
                ),
                forecastIncome = forecastIncome,
                requiredAmount = requiredAmount,
                variableMinimumAmount = requireNotNull(
                    record[Tables.BUDGET_CONSTRAINT_REVISIONS.VARIABLE_MINIMUM_AMOUNT],
                ),
                savingsFloorAmount = savingsFloor,
                maximumSavingsAmount = requireNotNull(
                    record[Tables.BUDGET_CONSTRAINT_REVISIONS.MAXIMUM_SAVINGS_AMOUNT],
                ),
                capacityAtSavingsFloor = forecastIncome - requiredAmount - savingsFloor,
                shortfall = requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.SHORTFALL_AMOUNT]),
                violations = objectMapper.readValue(
                    requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.VIOLATIONS]).data(),
                    object : TypeReference<List<BudgetPlanningViolation>>() {},
                ),
            ),
            constraintFingerprint = requireNotNull(
                record[Tables.BUDGET_CONSTRAINT_REVISIONS.CONSTRAINT_FINGERPRINT],
            ),
            planVersion = requireNotNull(record[Tables.BUDGET_PLANS.VERSION]),
            createdAt = requireNotNull(record[Tables.BUDGET_CONSTRAINT_REVISIONS.CREATED_AT]),
        )
    }

    private fun findCategories(constraintId: UUID, userId: UUID): List<BudgetCategoryConstraint> {
        val records = dsl.select(
            *Tables.BUDGET_CATEGORY_CONSTRAINTS.fields(),
            Tables.CATEGORIES.NAME,
            Tables.CATEGORIES.ICON,
            Tables.CATEGORIES.COLOR,
        )
            .from(Tables.BUDGET_CATEGORY_CONSTRAINTS)
            .join(Tables.CATEGORIES)
            .on(
                Tables.CATEGORIES.ID.eq(Tables.BUDGET_CATEGORY_CONSTRAINTS.CATEGORY_ID)
                    .and(Tables.CATEGORIES.USER_ID.eq(Tables.BUDGET_CATEGORY_CONSTRAINTS.USER_ID)),
            )
            .where(
                Tables.BUDGET_CATEGORY_CONSTRAINTS.CONSTRAINT_REVISION_ID.eq(constraintId)
                    .and(Tables.BUDGET_CATEGORY_CONSTRAINTS.USER_ID.eq(userId)),
            )
            .orderBy(Tables.CATEGORIES.NAME.asc())
            .fetch()
        val categoryIds = records.map { record -> requireNotNull(record[Tables.BUDGET_CATEGORY_CONSTRAINTS.ID]) }
        val levels = findFundingLevels(categoryIds, userId)
        return records.map { record -> toCategoryConstraint(record, levels) }
    }

    private fun findFundingLevels(
        categoryConstraintIds: Collection<UUID>,
        userId: UUID,
    ): Map<UUID, List<BudgetCategoryFundingLevel>> {
        if (categoryConstraintIds.isEmpty()) return emptyMap()
        return dsl.selectFrom(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS)
            .where(
                Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.CATEGORY_CONSTRAINT_ID.`in`(categoryConstraintIds)
                    .and(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.USER_ID.eq(userId)),
            )
            .orderBy(Tables.BUDGET_CONSTRAINT_FUNDING_LEVELS.COVERAGE.asc())
            .fetch { record ->
                requireNotNull(record.categoryConstraintId) to BudgetCategoryFundingLevel(
                    id = requireNotNull(record.id),
                    level = BudgetFundingLevel.valueOf(requireNotNull(record.level)),
                    amount = requireNotNull(record.amount),
                    coverage = requireNotNull(record.coverage),
                )
            }
            .groupBy({ (categoryId, _) -> categoryId }, { (_, level) -> level })
    }

    private fun toCategoryConstraint(
        record: Record,
        levels: Map<UUID, List<BudgetCategoryFundingLevel>>,
    ): BudgetCategoryConstraint {
        val id = requireNotNull(record[Tables.BUDGET_CATEGORY_CONSTRAINTS.ID])
        return BudgetCategoryConstraint(
            id = id,
            category = BudgetForecastCategory(
                id = requireNotNull(record[Tables.BUDGET_CATEGORY_CONSTRAINTS.CATEGORY_ID]),
                type = CategoryType.EXPENSE,
                name = requireNotNull(record[Tables.CATEGORIES.NAME]),
                icon = requireNotNull(record[Tables.CATEGORIES.ICON]),
                color = requireNotNull(record[Tables.CATEGORIES.COLOR]),
            ),
            allocationType = BudgetAllocationType.valueOf(
                requireNotNull(record[Tables.BUDGET_CATEGORY_CONSTRAINTS.ALLOCATION_TYPE]),
            ),
            constraintRole = BudgetConstraintRole.valueOf(
                requireNotNull(record[Tables.BUDGET_CATEGORY_CONSTRAINTS.CONSTRAINT_ROLE]),
            ),
            requiredAmount = requireNotNull(record[Tables.BUDGET_CATEGORY_CONSTRAINTS.REQUIRED_AMOUNT]),
            priority = record[Tables.BUDGET_CATEGORY_CONSTRAINTS.PRIORITY]?.let(BudgetPriority::valueOf),
            currentLimitAmount = requireNotNull(record[Tables.BUDGET_CATEGORY_CONSTRAINTS.CURRENT_LIMIT_AMOUNT]),
            fundingLevels = levels[id].orEmpty(),
            sourceKeys = objectMapper.readValue(
                requireNotNull(record[Tables.BUDGET_CATEGORY_CONSTRAINTS.SOURCE_KEYS]).data(),
                object : TypeReference<List<String>>() {},
            ),
        )
    }
}
