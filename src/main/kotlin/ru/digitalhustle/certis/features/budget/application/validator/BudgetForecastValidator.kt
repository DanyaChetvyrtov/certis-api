package ru.digitalhustle.certis.features.budget.application.validator

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.command.model.BudgetForecastManualAdjustmentData
import ru.digitalhustle.certis.features.budget.command.model.BudgetForecastOverrideData
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastOperationType
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningConflictException
import ru.digitalhustle.certis.features.budget.exceptions.InvalidBudgetException
import ru.digitalhustle.certis.features.budget.model.BudgetForecastPreview
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import java.util.UUID

@Component
class BudgetForecastValidator {

    fun validatePlan(plan: BudgetPlan, expectedVersion: Long) {
        if (plan.status != BudgetPlanStatus.DRAFT) {
            throw BudgetPlanningConflictException(
                message = "Only a draft budget plan can be changed",
                code = BudgetPlanningErrorCode.INVALID_BUDGET_PLAN_STATE,
                details = mapOf("planId" to plan.id, "status" to plan.status.name),
            )
        }
        if (plan.version != expectedVersion) {
            throw BudgetPlanningConflictException(
                message = "Budget plan was changed by another request",
                code = BudgetPlanningErrorCode.PLANNING_VERSION_CONFLICT,
                details = mapOf("expectedVersion" to expectedVersion, "currentVersion" to plan.version),
            )
        }
    }

    fun validateSourceFingerprint(preview: BudgetForecastPreview, expectedFingerprint: String) {
        if (preview.sourceFingerprint != expectedFingerprint) {
            throw BudgetPlanningConflictException(
                message = "Forecast sources changed; refresh the preview",
                code = BudgetPlanningErrorCode.FORECAST_SOURCE_CHANGED,
                details = mapOf(
                    "expectedSourceFingerprint" to expectedFingerprint,
                    "currentSourceFingerprint" to preview.sourceFingerprint,
                    "currentVersion" to preview.basedOnPlanVersion,
                ),
            )
        }
    }

    fun validateOverrides(preview: BudgetForecastPreview, overrides: List<BudgetForecastOverrideData>) {
        if (overrides.map(BudgetForecastOverrideData::sourceKey).distinct().size != overrides.size) {
            throw InvalidBudgetException("Forecast overrides must have unique source keys")
        }
        val sourceKeys = preview.items.map { it.sourceKey }.toSet()
        val unknownKeys = overrides.map(BudgetForecastOverrideData::sourceKey).filterNot(sourceKeys::contains)
        if (unknownKeys.isNotEmpty()) {
            throw InvalidBudgetException("Forecast override references an unknown source")
        }
    }

    fun validateManualAdjustments(
        preview: BudgetForecastPreview,
        adjustments: List<BudgetForecastManualAdjustmentData>,
        categories: Map<UUID, CategorySnapshot>,
    ) {
        val clientIds = adjustments.map(BudgetForecastManualAdjustmentData::clientId)
        if (clientIds.distinct().size != clientIds.size) {
            throw InvalidBudgetException("Manual adjustments must have unique client IDs")
        }
        val existingClientIds = preview.items.mapNotNull { it.manualClientId }.toSet()
        if (clientIds.any(existingClientIds::contains)) {
            throw InvalidBudgetException("Manual adjustment client ID is already used by this plan")
        }
        adjustments.forEach { adjustment -> validateCategory(adjustment, categories[adjustment.categoryId]) }
    }

    private fun validateCategory(
        adjustment: BudgetForecastManualAdjustmentData,
        category: CategorySnapshot?,
    ) {
        if (adjustment.categoryId == null) return
        requireNotNull(category)
        validateCategoryState(category)
        validateCategoryType(adjustment, category)
        validateTitle(adjustment)
    }

    private fun validateCategoryState(category: CategorySnapshot) {
        if (category.archivedAt != null) throw InvalidBudgetException("Manual adjustment category is archived")
    }

    private fun validateCategoryType(
        adjustment: BudgetForecastManualAdjustmentData,
        category: CategorySnapshot,
    ) {
        if (category.type.name != adjustment.operationType.name) {
            throw InvalidBudgetException("Manual adjustment category type does not match operation type")
        }
    }

    private fun validateTitle(adjustment: BudgetForecastManualAdjustmentData) {
        if (adjustment.operationType == BudgetForecastOperationType.EXPENSE && adjustment.title.isBlank()) {
            throw InvalidBudgetException("Manual adjustment title must not be blank")
        }
    }
}
