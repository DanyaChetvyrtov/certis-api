package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.BudgetAppliedBudgetDto
import ru.digitalhustle.certis.api.dto.BudgetAppliedOptimizationDto
import ru.digitalhustle.certis.api.dto.BudgetAppliedPlanDto

data class BudgetOptimizationApplyRs(

    val plan: BudgetAppliedPlanDto,

    val optimization: BudgetAppliedOptimizationDto,

    val budget: BudgetAppliedBudgetDto,
)
