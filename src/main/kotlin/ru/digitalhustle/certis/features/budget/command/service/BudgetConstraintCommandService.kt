package ru.digitalhustle.certis.features.budget.command.service

import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import java.time.OffsetDateTime
import java.util.UUID

interface BudgetConstraintCommandService {

    fun nextRevision(planId: UUID): Int

    fun save(constraints: BudgetConstraintSet)

    fun markGeneratedOptimizationsStale(planId: UUID, userId: UUID, staleAt: OffsetDateTime)
}
