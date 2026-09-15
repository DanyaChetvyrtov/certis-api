package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.BudgetFeasibilityDto
import ru.digitalhustle.certis.api.dto.BudgetForecastSummaryDto
import ru.digitalhustle.certis.api.dto.BudgetPlanCapabilitiesDto
import ru.digitalhustle.certis.api.dto.BudgetPlanConstraintStateDto
import ru.digitalhustle.certis.api.dto.BudgetPlanForecastStateDto
import ru.digitalhustle.certis.api.dto.BudgetPlanOptimizationStateDto
import ru.digitalhustle.certis.api.dto.BudgetPlanningViolationDto
import ru.digitalhustle.certis.api.dto.request.CreateBudgetPlanRq
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRevisionDto
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRevisionsRs
import ru.digitalhustle.certis.api.dto.response.BudgetPlanRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.budget.command.model.CreateBudgetPlanData
import ru.digitalhustle.certis.features.budget.model.BudgetPlanCapabilities
import ru.digitalhustle.certis.features.budget.model.BudgetPlanConstraintState
import ru.digitalhustle.certis.features.budget.model.BudgetPlanFeasibility
import ru.digitalhustle.certis.features.budget.model.BudgetPlanForecastState
import ru.digitalhustle.certis.features.budget.model.BudgetPlanForecastSummary
import ru.digitalhustle.certis.features.budget.model.BudgetPlanOptimizationState
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningViolation
import ru.digitalhustle.certis.features.budget.model.BudgetPlanRevision
import ru.digitalhustle.certis.features.budget.model.BudgetPlanRevisions
import ru.digitalhustle.certis.features.budget.model.BudgetPlanView
import java.time.YearMonth
import java.util.UUID

@Mapper(
    config = BaseMapperConfig::class,
    imports = [YearMonth::class],
)
interface BudgetPlanningMapper {

    @Mapping(target = "budgetMonth", expression = "java(source.getMonth().atDay(1))")
    fun convert(
        source: CreateBudgetPlanRq,
        userId: UUID,
        idempotencyKey: String,
    ): CreateBudgetPlanData

    @Mapping(target = "month", expression = "java(YearMonth.from(source.getBudgetMonth()).toString())")
    fun convert(source: BudgetPlanView): BudgetPlanRs

    fun convert(source: BudgetPlanRevisions): BudgetPlanRevisionsRs =
        BudgetPlanRevisionsRs(source.items.map { convert(it) })

    fun convert(source: BudgetPlanRevision): BudgetPlanRevisionDto

    fun convert(source: BudgetPlanForecastState): BudgetPlanForecastStateDto

    fun convert(source: BudgetPlanForecastSummary): BudgetForecastSummaryDto

    fun convert(source: BudgetPlanConstraintState): BudgetPlanConstraintStateDto

    fun convert(source: BudgetPlanFeasibility): BudgetFeasibilityDto

    fun convert(source: BudgetPlanningViolation): BudgetPlanningViolationDto

    fun convert(source: BudgetPlanOptimizationState): BudgetPlanOptimizationStateDto

    fun convert(source: BudgetPlanCapabilities): BudgetPlanCapabilitiesDto
}
