package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.BudgetConstraintCheckDto
import ru.digitalhustle.certis.api.dto.BudgetOptimizationDecisionDto
import ru.digitalhustle.certis.api.dto.BudgetOptimizationInputDto
import ru.digitalhustle.certis.api.dto.BudgetOptimizationResultDto
import ru.digitalhustle.certis.api.dto.BudgetOptimizationRunStatus
import ru.digitalhustle.certis.api.dto.BudgetPlanningViolationDto
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetOptimizationRunRs(

    val id: UUID,

    val planId: UUID,

    val status: BudgetOptimizationRunStatus,

    val algorithmVersion: String,

    val inputFingerprint: String,

    val input: BudgetOptimizationInputDto,

    val result: BudgetOptimizationResultDto?,

    val decisions: List<BudgetOptimizationDecisionDto>,

    val constraintChecks: List<BudgetConstraintCheckDto>,

    val violations: List<BudgetPlanningViolationDto>,

    val planVersion: Long,

    val createdAt: OffsetDateTime,

    val staleAt: OffsetDateTime?,

    val dismissedAt: OffsetDateTime?,

    val appliedAt: OffsetDateTime?,
)
