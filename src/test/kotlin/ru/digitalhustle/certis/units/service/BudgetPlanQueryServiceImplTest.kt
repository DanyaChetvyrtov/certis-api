package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.features.budget.enums.BudgetForecastStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningStep
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanNotFoundException
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.model.BudgetPlanStateSnapshot
import ru.digitalhustle.certis.features.budget.query.repository.BudgetPlanQueryRepository
import ru.digitalhustle.certis.features.budget.query.service.impl.BudgetPlanQueryServiceImpl
import ru.digitalhustle.certis.features.budget.util.BudgetPlanViewFactory
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BudgetPlanQueryServiceImplTest {

    private val repository = mock(BudgetPlanQueryRepository::class.java)
    private val service = BudgetPlanQueryServiceImpl(repository, BudgetPlanViewFactory())

    @Test
    fun `should return current plan with derived forecast state`() {
        // given
        val plan = createPlan()
        `when`(repository.findCurrentByUserIdAndScope(plan.userId, plan.budgetMonth, plan.currency))
            .thenReturn(plan)
        `when`(repository.findStatesByPlanIds(listOf(plan.id)))
            .thenReturn(mapOf(plan.id to BudgetPlanStateSnapshot.EMPTY))

        // when
        val result = service.getCurrent(plan.userId, plan.budgetMonth, plan.currency)

        // then
        assertThat(result.id).isEqualTo(plan.id)
        assertThat(result.currentStep).isEqualTo(BudgetPlanningStep.FORECAST)
        assertThat(result.forecast.status).isEqualTo(BudgetForecastStatus.MISSING)
        assertThat(result.capabilities.canEditForecast).isTrue()
    }

    @Test
    fun `should hide missing or another users plan`() {
        // given
        val planId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        `when`(repository.findByIdAndUserId(planId, userId)).thenReturn(null)

        // when, then
        assertThatThrownBy { service.getById(planId, userId) }
            .isInstanceOf(BudgetPlanNotFoundException::class.java)
    }

    @Test
    fun `should build revision list from one batched state lookup`() {
        // given
        val latest = createPlan(revision = 2)
        val previous = createPlan(
            userId = latest.userId,
            revision = 1,
            status = BudgetPlanStatus.CANCELLED,
        )
        val plans = listOf(latest, previous)
        val states = plans.associate { plan -> plan.id to BudgetPlanStateSnapshot.EMPTY }
        `when`(repository.findAllByUserIdAndScope(latest.userId, latest.budgetMonth, latest.currency))
            .thenReturn(plans)
        `when`(repository.findStatesByPlanIds(plans.map(BudgetPlan::id))).thenReturn(states)

        // when
        val result = service.getRevisions(latest.userId, latest.budgetMonth, latest.currency)

        // then
        assertThat(result.items.map { item -> item.id }).containsExactly(latest.id, previous.id)
        verify(repository).findStatesByPlanIds(plans.map(BudgetPlan::id))
    }

    private fun createPlan(
        userId: UUID = UUID.randomUUID(),
        revision: Int = 1,
        status: BudgetPlanStatus = BudgetPlanStatus.DRAFT,
    ): BudgetPlan {
        val createdAt = OffsetDateTime.parse("2026-09-15T12:00:00Z")

        return BudgetPlan(
            id = UUID.randomUUID(),
            userId = userId,
            previousPlanId = null,
            baselineBudgetId = null,
            appliedBudgetId = null,
            budgetMonth = LocalDate.parse("2026-10-01"),
            currency = Currency.RUB,
            revision = revision,
            version = 0,
            status = status,
            idempotencyKey = "plan-$revision",
            createdAt = createdAt,
            updatedAt = createdAt,
            appliedAt = null,
            supersededAt = null,
            cancelledAt = if (status == BudgetPlanStatus.CANCELLED) createdAt else null,
        )
    }
}
