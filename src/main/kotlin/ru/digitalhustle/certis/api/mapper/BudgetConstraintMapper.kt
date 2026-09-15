package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.api.dto.BudgetCategoryConstraintDto
import ru.digitalhustle.certis.api.dto.BudgetFeasibilityDto
import ru.digitalhustle.certis.api.dto.BudgetFundingLevelDto
import ru.digitalhustle.certis.api.dto.BudgetPlanningViolationDto
import ru.digitalhustle.certis.api.dto.CategoryOptionDto
import ru.digitalhustle.certis.api.dto.request.BudgetCategoryConstraintRq
import ru.digitalhustle.certis.api.dto.request.BudgetFundingLevelRq
import ru.digitalhustle.certis.api.dto.request.SaveBudgetConstraintsRq
import ru.digitalhustle.certis.api.dto.response.BudgetConstraintSetRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.budget.command.model.BudgetCategoryConstraintData
import ru.digitalhustle.certis.features.budget.command.model.BudgetFundingLevelData
import ru.digitalhustle.certis.features.budget.command.model.SaveBudgetConstraintsData
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryConstraint
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryFundingLevel
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.model.BudgetPlanFeasibility
import ru.digitalhustle.certis.features.budget.model.BudgetPlanningViolation
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface BudgetConstraintMapper {

    fun convert(
        source: SaveBudgetConstraintsRq,
        planId: UUID,
        userId: UUID,
    ): SaveBudgetConstraintsData

    fun convert(source: BudgetCategoryConstraintRq): BudgetCategoryConstraintData

    fun convert(source: BudgetFundingLevelRq): BudgetFundingLevelData

    fun convert(source: BudgetConstraintSet): BudgetConstraintSetRs

    fun convert(source: BudgetCategoryConstraint): BudgetCategoryConstraintDto

    fun convert(source: BudgetCategoryFundingLevel): BudgetFundingLevelDto

    fun convert(source: BudgetForecastCategory): CategoryOptionDto

    fun convert(source: BudgetPlanFeasibility): BudgetFeasibilityDto

    fun convert(source: BudgetPlanningViolation): BudgetPlanningViolationDto
}
