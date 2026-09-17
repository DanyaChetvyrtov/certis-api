package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.BudgetConstraintCheckDto
import ru.digitalhustle.certis.api.dto.BudgetOptimizationDecisionDto
import ru.digitalhustle.certis.api.dto.BudgetOptimizationInputDto
import ru.digitalhustle.certis.api.dto.BudgetOptimizationReasonDto
import ru.digitalhustle.certis.api.dto.BudgetOptimizationResultDto
import ru.digitalhustle.certis.api.dto.BudgetPlanningViolationDto
import ru.digitalhustle.certis.api.dto.CategoryOptionDto
import ru.digitalhustle.certis.api.dto.request.DismissBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.request.GenerateBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationDismissRs
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationRunRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.budget.command.model.DismissBudgetPlanningOptimizationData
import ru.digitalhustle.certis.features.budget.command.model.GenerateBudgetPlanningOptimizationData
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningConstraintCheck
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationDecision
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationDismissal
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationInput
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationReason
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationResult
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningOptimizationRun
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningViolation
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface BudgetPlanningOptimizationMapper {

    fun convert(
        source: GenerateBudgetOptimizationRq,
        planId: UUID,
        userId: UUID,
        idempotencyKey: String,
    ): GenerateBudgetPlanningOptimizationData

    fun convert(
        source: DismissBudgetOptimizationRq,
        planId: UUID,
        optimizationId: UUID,
        userId: UUID,
    ): DismissBudgetPlanningOptimizationData

    fun convert(source: BudgetPlanningOptimizationRun): BudgetOptimizationRunRs

    @Mapping(target = "currentStep", constant = "OPTIMIZE")
    fun convert(source: BudgetPlanningOptimizationDismissal): BudgetOptimizationDismissRs

    fun convert(source: BudgetPlanningOptimizationInput): BudgetOptimizationInputDto

    fun convert(source: BudgetPlanningOptimizationResult): BudgetOptimizationResultDto

    fun convert(source: BudgetPlanningOptimizationDecision): BudgetOptimizationDecisionDto

    fun convert(source: BudgetPlanningOptimizationReason): BudgetOptimizationReasonDto

    fun convert(source: BudgetPlanningConstraintCheck): BudgetConstraintCheckDto

    fun convert(source: BudgetPlanningViolation): BudgetPlanningViolationDto

    fun convert(source: BudgetForecastCategory): CategoryOptionDto
}
