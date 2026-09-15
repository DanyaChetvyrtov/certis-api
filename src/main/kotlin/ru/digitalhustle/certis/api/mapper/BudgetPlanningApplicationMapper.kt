package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.BudgetAppliedAllocationDto
import ru.digitalhustle.certis.api.dto.BudgetAppliedBudgetDto
import ru.digitalhustle.certis.api.dto.BudgetAppliedOptimizationDto
import ru.digitalhustle.certis.api.dto.BudgetAppliedPlanDto
import ru.digitalhustle.certis.api.dto.request.ApplyBudgetOptimizationRq
import ru.digitalhustle.certis.api.dto.response.BudgetOptimizationApplyRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.budget.command.model.ApplyBudgetPlanningOptimizationData
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningApplicationResult
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningAppliedAllocation
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningAppliedBudget
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningAppliedOptimization
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningAppliedPlan
import java.time.YearMonth
import java.util.UUID

@Mapper(
    config = BaseMapperConfig::class,
    imports = [YearMonth::class],
)
interface BudgetPlanningApplicationMapper {

    fun convert(
        source: ApplyBudgetOptimizationRq,
        planId: UUID,
        optimizationId: UUID,
        userId: UUID,
        idempotencyKey: String,
    ): ApplyBudgetPlanningOptimizationData

    fun convert(source: BudgetPlanningApplicationResult): BudgetOptimizationApplyRs

    fun convert(source: BudgetPlanningAppliedPlan): BudgetAppliedPlanDto

    fun convert(source: BudgetPlanningAppliedOptimization): BudgetAppliedOptimizationDto

    @Mapping(target = "month", expression = "java(YearMonth.from(source.getBudgetMonth()).toString())")
    fun convert(source: BudgetPlanningAppliedBudget): BudgetAppliedBudgetDto

    fun convert(source: BudgetPlanningAppliedAllocation): BudgetAppliedAllocationDto
}
