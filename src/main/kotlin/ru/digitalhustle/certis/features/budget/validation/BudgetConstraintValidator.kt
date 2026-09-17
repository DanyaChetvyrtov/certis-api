package ru.digitalhustle.certis.features.budget.validation

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.command.model.BudgetCategoryConstraintData
import ru.digitalhustle.certis.features.budget.command.model.SaveBudgetConstraintsData
import ru.digitalhustle.certis.features.budget.enums.BudgetAllocationType
import ru.digitalhustle.certis.features.budget.enums.BudgetConstraintRole
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastOperationType
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningConflictException
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningValidationException
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.category.api.CategorySnapshot
import ru.digitalhustle.certis.features.category.enums.CategoryType
import java.math.BigDecimal
import java.util.UUID

@Component
class BudgetConstraintValidator {

    fun validateForecastSources(forecast: BudgetForecast) {
        val uncategorized = forecast.items.filter { item ->
            item.included &&
                item.operationType == BudgetForecastOperationType.EXPENSE &&
                item.category == null
        }
        if (uncategorized.any { item -> item.defaultConstraintRole == BudgetConstraintRole.REQUIRED }) {
            throw validationException(
                message = "Every required forecast occurrence must have an expense category",
                code = BudgetPlanningErrorCode.UNCATEGORIZED_REQUIRED_OCCURRENCE,
                details = mapOf("sourceKeys" to uncategorized.map { item -> item.sourceKey }),
            )
        }
        if (uncategorized.isNotEmpty()) {
            throw validationException(
                message = "Every included expense source must have a category",
                code = BudgetPlanningErrorCode.CONSTRAINTS_INCOMPLETE,
                details = mapOf("sourceKeys" to uncategorized.map { item -> item.sourceKey }),
            )
        }
    }

    fun validateCurrentForecast(forecast: BudgetForecast, expectedRevision: Int? = null) {
        if (forecast.status != BudgetForecastStatus.CURRENT) {
            throw BudgetPlanningConflictException(
                message = "Forecast sources changed; confirm a new forecast revision",
                code = BudgetPlanningErrorCode.FORECAST_SOURCE_CHANGED,
                details = mapOf("currentForecastRevision" to forecast.revision),
            )
        }
        if (expectedRevision != null && forecast.revision != expectedRevision) {
            throw BudgetPlanningConflictException(
                message = "Forecast revision changed; refresh constraints",
                code = BudgetPlanningErrorCode.FORECAST_SOURCE_CHANGED,
                details = mapOf(
                    "expectedForecastRevision" to expectedRevision,
                    "currentForecastRevision" to forecast.revision,
                ),
            )
        }
    }

    fun validateRequest(
        data: SaveBudgetConstraintsData,
        forecast: BudgetForecast,
        suggestion: BudgetConstraintSet,
        categories: Map<UUID, CategorySnapshot>,
    ) {
        validateCategorySet(data, suggestion)
        validateCategoryOwnership(data, categories)
        val requiredByCategory = forecast.items
            .filter { item ->
                item.included &&
                    item.operationType == BudgetForecastOperationType.EXPENSE &&
                    item.defaultConstraintRole == BudgetConstraintRole.REQUIRED
            }
            .groupBy { item -> requireNotNull(item.category).id }
            .mapValues { (_, items) -> items.fold(BigDecimal.ZERO) { total, item -> total + item.effectiveAmount } }
        data.categories.forEach { category ->
            validateShape(category)
            validateRequiredAmount(category, requiredByCategory[category.categoryId] ?: BigDecimal.ZERO)
        }
    }

    fun validateCategorySet(data: SaveBudgetConstraintsData, suggestion: BudgetConstraintSet) {
        val actual = data.categories.map { category -> category.categoryId }
        val required = suggestion.categories.map { category -> category.category.id }.toSet()
        val missing = required - actual.toSet()
        if (actual.distinct().size != actual.size || missing.isNotEmpty()) {
            throw validationException(
                message =
                    "Every forecast expense category is required once; extra active expense categories are allowed",
                code = BudgetPlanningErrorCode.CONSTRAINTS_INCOMPLETE,
                details = mapOf(
                    "requiredCategoryIds" to required,
                    "missingCategoryIds" to missing,
                    "actualCategoryIds" to actual,
                ),
            )
        }
    }

    private fun validateCategoryOwnership(
        data: SaveBudgetConstraintsData,
        categories: Map<UUID, CategorySnapshot>,
    ) {
        val invalid = data.categories.map { category -> category.categoryId }.filter { categoryId ->
            categories[categoryId]?.let { category ->
                category.archivedAt != null || category.type != CategoryType.EXPENSE
            } ?: true
        }
        if (invalid.isNotEmpty()) {
            throw validationException(
                message = "Constraint categories must be active expense categories owned by the user",
                code = BudgetPlanningErrorCode.CONSTRAINTS_INCOMPLETE,
                details = mapOf("categoryIds" to invalid),
            )
        }
    }

    private fun validateShape(category: BudgetCategoryConstraintData) {
        when (category.allocationType) {
            BudgetAllocationType.FIXED -> validateFixedShape(category)
            BudgetAllocationType.VARIABLE -> validateVariableShape(category)
        }
    }

    private fun validateFixedShape(category: BudgetCategoryConstraintData) {
        if (
            category.constraintRole != BudgetConstraintRole.REQUIRED ||
            category.priority != null ||
            category.fundingLevels.isNotEmpty()
        ) {
            throw invalidFundingLevels(category.categoryId, "FIXED categories must be REQUIRED without funding levels")
        }
    }

    private fun validateVariableShape(category: BudgetCategoryConstraintData) {
        if (category.priority == null || category.fundingLevels.isEmpty()) {
            throw invalidFundingLevels(category.categoryId, "VARIABLE categories require a priority and funding levels")
        }
        if (category.constraintRole == BudgetConstraintRole.FLEXIBLE && category.requiredAmount.signum() != 0) {
            throw invalidFundingLevels(category.categoryId, "FLEXIBLE categories cannot define a required amount")
        }
        validateFundingLevels(category)
    }

    private fun validateFundingLevels(category: BudgetCategoryConstraintData) {
        val levels = category.fundingLevels
        val ordered = levels.sortedBy { level -> level.level.ordinal }
        val error = when {
            levels.map { level -> level.level }.distinct().size != levels.size ->
                "Funding level names must be unique"
            ordered.zipWithNext().any { (current, next) -> current.amount > next.amount } ->
                "Funding level amounts must be non-decreasing"
            levels.any { level -> level.amount < category.requiredAmount } ->
                "Funding level amount cannot be below the required amount"
            else -> null
        }
        if (error != null) {
            throw invalidFundingLevels(category.categoryId, error)
        }
    }

    private fun validateRequiredAmount(category: BudgetCategoryConstraintData, sourceRequiredAmount: BigDecimal) {
        if (category.requiredAmount < sourceRequiredAmount) {
            throw validationException(
                message = "Required forecast expenses cannot be reduced",
                code = BudgetPlanningErrorCode.CONSTRAINTS_INCOMPLETE,
                details = mapOf(
                    "categoryId" to category.categoryId,
                    "requiredAmount" to category.requiredAmount,
                    "sourceRequiredAmount" to sourceRequiredAmount,
                ),
            )
        }
    }

    private fun invalidFundingLevels(categoryId: UUID, message: String): BudgetPlanningValidationException =
        validationException(
            message = message,
            code = BudgetPlanningErrorCode.INVALID_FUNDING_LEVELS,
            details = mapOf("categoryId" to categoryId),
        )

    private fun validationException(
        message: String,
        code: BudgetPlanningErrorCode,
        details: Map<String, Any?>,
    ): BudgetPlanningValidationException =
        BudgetPlanningValidationException(message = message, code = code, details = details)
}
