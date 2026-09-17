package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.features.budget.command.model.CreateBudgetPlanData
import ru.digitalhustle.certis.features.budget.command.repository.BudgetPlanRepository
import ru.digitalhustle.certis.features.budget.command.repository.BudgetRepository
import ru.digitalhustle.certis.features.budget.command.service.impl.BudgetPlanCommandServiceImpl
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanStatus
import ru.digitalhustle.certis.features.budget.enums.BudgetPlanningErrorCode
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanningConflictException
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.shared.enums.Currency
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class BudgetPlanCommandServiceImplTest {

    private val repository = mock(BudgetPlanRepository::class.java)
    private val budgetRepository = mock(BudgetRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC)
    private val service = BudgetPlanCommandServiceImpl(repository, budgetRepository, ApplicationClock(clock))

    @Test
    fun `should create next plan revision with normalized idempotency key and baseline`() {
        // given
        val data = createData(idempotencyKey = " plan-key ")
        val previousPlan = createPlan(revision = 2, status = BudgetPlanStatus.CANCELLED)
        val baselineBudgetId = UUID.randomUUID()
        val captor = ArgumentCaptor.forClass(BudgetPlan::class.java)

        `when`(repository.findByUserIdAndIdempotencyKey(data.userId, "plan-key")).thenReturn(null)
        `when`(repository.findActiveDraft(data.userId, data.budgetMonth, data.currency)).thenReturn(null)
        `when`(repository.findLatestByScopeForUpdate(data.userId, data.budgetMonth, data.currency))
            .thenReturn(previousPlan)
        `when`(
            budgetRepository.findIdByUserIdAndMonthAndCurrency(data.userId, data.budgetMonth, data.currency),
        ).thenReturn(baselineBudgetId)
        `when`(repository.insertOrFindByIdempotencyKey(capturePlan(captor)))
            .thenAnswer { captor.value }

        // when
        val result = service.create(data)

        // then
        assertThat(result.previousPlanId).isEqualTo(previousPlan.id)
        assertThat(result.baselineBudgetId).isEqualTo(baselineBudgetId)
        assertThat(result.revision).isEqualTo(3)
        assertThat(result.version).isZero()
        assertThat(result.status).isEqualTo(BudgetPlanStatus.DRAFT)
        assertThat(result.idempotencyKey).isEqualTo("plan-key")
        assertThat(result.createdAt).isEqualTo(OffsetDateTime.now(clock))
        assertThat(result.updatedAt).isEqualTo(OffsetDateTime.now(clock))
    }

    @Test
    fun `should return existing plan for matching idempotent replay`() {
        // given
        val data = createData()
        val existing = createPlan(userId = data.userId, idempotencyKey = data.idempotencyKey)
        `when`(repository.findByUserIdAndIdempotencyKey(data.userId, data.idempotencyKey))
            .thenReturn(existing)

        // when
        val result = service.create(data)

        // then
        assertThat(result).isEqualTo(existing)
        verify(repository).findByUserIdAndIdempotencyKey(data.userId, data.idempotencyKey)
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `should reject idempotency key reused for another scope`() {
        // given
        val data = createData()
        val existing = createPlan(
            userId = data.userId,
            budgetMonth = data.budgetMonth.plusMonths(1),
            idempotencyKey = data.idempotencyKey,
        )
        `when`(repository.findByUserIdAndIdempotencyKey(data.userId, data.idempotencyKey))
            .thenReturn(existing)

        // when
        val exception = assertThrows<BudgetPlanningConflictException> { service.create(data) }

        // then
        assertThat(exception.code).isEqualTo(BudgetPlanningErrorCode.IDEMPOTENCY_KEY_REUSED)
    }

    @Test
    fun `should reject another plan for active scope`() {
        // given
        val data = createData()
        val activePlan = createPlan(userId = data.userId)
        `when`(repository.findByUserIdAndIdempotencyKey(data.userId, data.idempotencyKey)).thenReturn(null)
        `when`(repository.findActiveDraft(data.userId, data.budgetMonth, data.currency)).thenReturn(activePlan)

        // when
        val exception = assertThrows<BudgetPlanningConflictException> { service.create(data) }

        // then
        assertThat(exception.code).isEqualTo(BudgetPlanningErrorCode.ACTIVE_BUDGET_PLAN_EXISTS)
    }

    private fun createData(idempotencyKey: String = "plan-key"): CreateBudgetPlanData =
        CreateBudgetPlanData(
            userId = UUID.randomUUID(),
            budgetMonth = LocalDate.parse("2026-10-01"),
            currency = Currency.RUB,
            idempotencyKey = idempotencyKey,
        )

    private fun createPlan(
        userId: UUID = UUID.randomUUID(),
        budgetMonth: LocalDate = LocalDate.parse("2026-10-01"),
        revision: Int = 1,
        status: BudgetPlanStatus = BudgetPlanStatus.DRAFT,
        idempotencyKey: String = "existing-key",
    ): BudgetPlan {
        val createdAt = OffsetDateTime.parse("2026-09-14T12:00:00Z")

        return BudgetPlan(
            id = UUID.randomUUID(),
            userId = userId,
            previousPlanId = null,
            baselineBudgetId = null,
            appliedBudgetId = null,
            budgetMonth = budgetMonth,
            currency = Currency.RUB,
            revision = revision,
            version = 0,
            status = status,
            idempotencyKey = idempotencyKey,
            createdAt = createdAt,
            updatedAt = createdAt,
            appliedAt = null,
            supersededAt = null,
            cancelledAt = if (status == BudgetPlanStatus.CANCELLED) createdAt else null,
        )
    }

    private fun capturePlan(captor: ArgumentCaptor<BudgetPlan>): BudgetPlan {
        captor.capture()
        return createPlan()
    }
}
