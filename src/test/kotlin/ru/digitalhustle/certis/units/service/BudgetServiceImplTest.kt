package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.BudgetAllocationStatus
import ru.digitalhustle.certis.enums.BudgetExpenseType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.BudgetOptimizationConflictException
import ru.digitalhustle.certis.exception.custom.InvalidBudgetException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.budget.ApplyBudgetOptimizationData
import ru.digitalhustle.certis.model.budget.BudgetAllocationDetails
import ru.digitalhustle.certis.model.budget.BudgetDetails
import ru.digitalhustle.certis.model.budget.BudgetOptimizationInputAllocation
import ru.digitalhustle.certis.model.budget.BudgetOptimizationInputSnapshot
import ru.digitalhustle.certis.model.budget.SaveBudgetAllocationData
import ru.digitalhustle.certis.model.budget.SaveBudgetData
import ru.digitalhustle.certis.model.entity.Budget
import ru.digitalhustle.certis.repository.BudgetRepository
import ru.digitalhustle.certis.service.domain.impl.BudgetServiceImpl
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class BudgetServiceImplTest {

    private val budgetRepository = mock(BudgetRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-15T10:15:30Z"), ZoneOffset.UTC)
    private val budgetService = BudgetServiceImpl(budgetRepository, clock)

    private companion object {
        private val BUDGET_MONTH = LocalDate.parse("2026-08-01")
        private val MONTH_START = OffsetDateTime.parse("2026-08-01T00:00:00Z")
        private val NEXT_MONTH_START = OffsetDateTime.parse("2026-09-01T00:00:00Z")
    }

    @Test
    fun `should get budget details for user and month`() {
        // given
        val details = createBudgetDetails()
        val userId = UUID.randomUUID()

        `when`(
            budgetRepository.findDetailsByUserIdAndMonth(
                userId,
                BUDGET_MONTH,
                MONTH_START,
                NEXT_MONTH_START,
            ),
        ).thenReturn(details)

        // when
        val result = budgetService.getByMonth(userId, BUDGET_MONTH)

        // then
        assertThat(result).isEqualTo(details)
    }

    @Test
    fun `should hide missing or another user's budget`() {
        // given
        val userId = UUID.randomUUID()

        `when`(
            budgetRepository.findDetailsByUserIdAndMonth(
                userId,
                BUDGET_MONTH,
                MONTH_START,
                NEXT_MONTH_START,
            ),
        ).thenReturn(null)

        // when, then
        assertThatThrownBy {
            budgetService.getByMonth(userId, BUDGET_MONTH)
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `should create budget aggregate`() {
        // given
        val budget = createSaveBudgetData()
        val details = createBudgetDetails()
        val budgetCaptor = ArgumentCaptor.forClass(Budget::class.java)

        `when`(budgetRepository.findPreferredCurrencyByUserIdForUpdate(budget.userId))
            .thenReturn(Currency.RUB)
        `when`(
            budgetRepository.countActiveExpenseCategories(
                budget.userId,
                budget.allocations.map { allocation -> allocation.categoryId },
            ),
        ).thenReturn(1)
        `when`(budgetRepository.findByUserIdAndMonthForUpdate(budget.userId, budget.budgetMonth))
            .thenReturn(null)
        `when`(budgetRepository.insert(captureBudget(budgetCaptor)))
            .thenAnswer { budgetCaptor.value }
        `when`(
            budgetRepository.findDetailsByUserIdAndMonth(
                budget.userId,
                BUDGET_MONTH,
                MONTH_START,
                NEXT_MONTH_START,
            ),
        ).thenReturn(details)

        // when
        val result = budgetService.save(budget)

        // then
        assertThat(result).isEqualTo(details)
        assertThat(budgetCaptor.value.userId).isEqualTo(budget.userId)
        assertThat(budgetCaptor.value.currency).isEqualTo(Currency.RUB)
        assertThat(budgetCaptor.value.createdAt).isEqualTo(OffsetDateTime.now(clock))
        verify(budgetRepository).insertAllocations(anyCollection())
        verify(budgetRepository, never()).deleteAllocations(details.id)
    }

    @Test
    fun `should preserve existing budget currency when preference changes`() {
        // given
        val budget = createSaveBudgetData()
        val currentBudget = createBudget(
            userId = budget.userId,
            currency = Currency.EUR,
        )
        val details = createBudgetDetails().copy(
            id = currentBudget.id,
            currency = Currency.EUR,
        )
        val budgetCaptor = ArgumentCaptor.forClass(Budget::class.java)

        `when`(budgetRepository.findPreferredCurrencyByUserIdForUpdate(budget.userId))
            .thenReturn(Currency.RUB)
        `when`(
            budgetRepository.countActiveExpenseCategories(
                budget.userId,
                budget.allocations.map { allocation -> allocation.categoryId },
            ),
        ).thenReturn(1)
        `when`(budgetRepository.findByUserIdAndMonthForUpdate(budget.userId, budget.budgetMonth))
            .thenReturn(currentBudget)
        `when`(budgetRepository.update(captureBudget(budgetCaptor)))
            .thenAnswer { budgetCaptor.value }
        `when`(
            budgetRepository.findDetailsByUserIdAndMonth(
                budget.userId,
                BUDGET_MONTH,
                MONTH_START,
                NEXT_MONTH_START,
            ),
        ).thenReturn(details)

        // when
        val result = budgetService.save(budget)

        // then
        assertThat(result.currency).isEqualTo(Currency.EUR)
        assertThat(budgetCaptor.value.currency).isEqualTo(Currency.EUR)
        verify(budgetRepository).deleteAllocations(currentBudget.id)
    }

    @Test
    fun `should atomically apply optimization to unchanged budget`() {
        // given
        val userId = UUID.randomUUID()
        val currentBudget = createBudget(userId, Currency.RUB)
        val currentDetails = createBudgetDetails().copy(
            id = currentBudget.id,
            allocations = listOf(createAllocationDetails()),
        )
        val optimizedDetails = currentDetails.copy(
            allocations = currentDetails.allocations.map { allocation ->
                allocation.copy(limitAmount = BigDecimal("80.0000"))
            },
        )
        val budgetCaptor = ArgumentCaptor.forClass(Budget::class.java)
        val data = ApplyBudgetOptimizationData(
            userId = userId,
            inputSnapshot = currentDetails.toInputSnapshot(),
            allocations = listOf(
                SaveBudgetAllocationData(
                    categoryId = currentDetails.allocations.single().categoryId,
                    expenseType = BudgetExpenseType.VARIABLE,
                    limitAmount = BigDecimal("80.0000"),
                ),
            ),
        )

        `when`(budgetRepository.findByUserIdAndMonthForUpdate(userId, BUDGET_MONTH))
            .thenReturn(currentBudget)
        `when`(
            budgetRepository.findDetailsByUserIdAndMonth(
                userId,
                BUDGET_MONTH,
                MONTH_START,
                NEXT_MONTH_START,
            ),
        ).thenReturn(currentDetails, optimizedDetails)
        `when`(
            budgetRepository.countActiveExpenseCategories(
                userId,
                listOf(currentDetails.allocations.single().categoryId),
            ),
        ).thenReturn(1)
        `when`(budgetRepository.update(captureBudget(budgetCaptor)))
            .thenReturn(currentBudget)

        // when
        val result = budgetService.applyOptimization(data)

        // then
        assertThat(result).isEqualTo(optimizedDetails)
        verify(budgetRepository).deleteAllocations(currentBudget.id)
        verify(budgetRepository).insertAllocations(anyCollection())
    }

    @Test
    fun `should reject optimization when budget state has changed`() {
        // given
        val userId = UUID.randomUUID()
        val currentBudget = createBudget(userId, Currency.RUB)
        val originalDetails = createBudgetDetails().copy(
            id = currentBudget.id,
            allocations = listOf(createAllocationDetails()),
        )
        val changedDetails = originalDetails.copy(plannedIncome = BigDecimal("1200.0000"))
        val data = ApplyBudgetOptimizationData(
            userId = userId,
            inputSnapshot = originalDetails.toInputSnapshot(),
            allocations = listOf(createAllocation()),
        )

        `when`(budgetRepository.findByUserIdAndMonthForUpdate(userId, BUDGET_MONTH))
            .thenReturn(currentBudget)
        `when`(
            budgetRepository.findDetailsByUserIdAndMonth(
                userId,
                BUDGET_MONTH,
                MONTH_START,
                NEXT_MONTH_START,
            ),
        ).thenReturn(changedDetails)

        // when, then
        assertThatThrownBy {
            budgetService.applyOptimization(data)
        }
            .isInstanceOf(BudgetOptimizationConflictException::class.java)
            .hasMessage(ErrorMessages.BUDGET_OPTIMIZATION_STALE)

        verify(budgetRepository, never()).deleteAllocations(currentBudget.id)
        verify(budgetRepository, never()).insertAllocations(anyCollection())
    }

    @Test
    fun `should reject duplicate allocation categories before persistence`() {
        // given
        val allocation = createAllocation()
        val budget = createSaveBudgetData().copy(allocations = listOf(allocation, allocation))

        // when, then
        assertThatThrownBy {
            budgetService.save(budget)
        }
            .isInstanceOf(InvalidBudgetException::class.java)
            .hasMessage(ErrorMessages.BUDGET_DUPLICATE_CATEGORIES)

        verifyNoInteractions(budgetRepository)
    }

    @Test
    fun `should reject budget whose allocations and savings exceed income`() {
        // given
        val budget = createSaveBudgetData().copy(
            plannedIncome = BigDecimal("250.00"),
        )

        // when, then
        assertThatThrownBy {
            budgetService.save(budget)
        }
            .isInstanceOf(InvalidBudgetException::class.java)
            .hasMessage(ErrorMessages.BUDGET_ALLOCATIONS_EXCEED_INCOME)

        verifyNoInteractions(budgetRepository)
    }

    @Test
    fun `should reject category that is not an active owned expense category`() {
        // given
        val budget = createSaveBudgetData()

        `when`(budgetRepository.findPreferredCurrencyByUserIdForUpdate(budget.userId))
            .thenReturn(Currency.RUB)
        `when`(
            budgetRepository.countActiveExpenseCategories(
                budget.userId,
                budget.allocations.map { allocation -> allocation.categoryId },
            ),
        ).thenReturn(0)

        // when, then
        assertThatThrownBy {
            budgetService.save(budget)
        }
            .isInstanceOf(InvalidBudgetException::class.java)
            .hasMessage(ErrorMessages.BUDGET_CATEGORY_INVALID)

        verify(budgetRepository, never())
            .findByUserIdAndMonthForUpdate(budget.userId, budget.budgetMonth)
    }

    private fun createSaveBudgetData(): SaveBudgetData =
        SaveBudgetData(
            userId = UUID.randomUUID(),
            budgetMonth = BUDGET_MONTH,
            plannedIncome = BigDecimal("1000.00"),
            savingsTarget = BigDecimal("200.00"),
            allocations = listOf(createAllocation()),
        )

    private fun createAllocation(): SaveBudgetAllocationData =
        SaveBudgetAllocationData(
            categoryId = UUID.randomUUID(),
            expenseType = BudgetExpenseType.VARIABLE,
            limitAmount = BigDecimal("100.00"),
        )

    private fun createAllocationDetails(): BudgetAllocationDetails =
        BudgetAllocationDetails(
            id = UUID.randomUUID(),
            categoryId = UUID.randomUUID(),
            categoryName = "Groceries",
            categoryIcon = "wallet",
            categoryColor = "#10B981",
            expenseType = BudgetExpenseType.VARIABLE,
            limitAmount = BigDecimal("100.0000"),
            spentAmount = BigDecimal("40.0000"),
            status = BudgetAllocationStatus.ON_TRACK,
        )

    private fun BudgetDetails.toInputSnapshot(): BudgetOptimizationInputSnapshot =
        BudgetOptimizationInputSnapshot(
            budgetId = id,
            budgetMonth = budgetMonth,
            plannedIncome = plannedIncome,
            savingsTarget = savingsTarget,
            currency = currency,
            budgetUpdatedAt = updatedAt,
            allocations = allocations.map { allocation ->
                BudgetOptimizationInputAllocation(
                    allocationId = allocation.id,
                    categoryId = allocation.categoryId,
                    expenseType = allocation.expenseType,
                    limitAmount = allocation.limitAmount,
                    spentAmount = allocation.spentAmount,
                )
            },
        )

    private fun createBudgetDetails(): BudgetDetails =
        BudgetDetails(
            id = UUID.randomUUID(),
            budgetMonth = BUDGET_MONTH,
            plannedIncome = BigDecimal("1000.00"),
            savingsTarget = BigDecimal("200.00"),
            currency = Currency.RUB,
            allocations = emptyList(),
            createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            updatedAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
        )

    private fun createBudget(
        userId: UUID,
        currency: Currency,
    ): Budget =
        Budget(
            id = UUID.randomUUID(),
            userId = userId,
            budgetMonth = BUDGET_MONTH,
            plannedIncome = BigDecimal("1000.00"),
            savingsTarget = BigDecimal("200.00"),
            currency = currency,
            createdAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
            updatedAt = OffsetDateTime.parse("2026-08-01T10:00:00Z"),
        )

    private fun captureBudget(captor: ArgumentCaptor<Budget>): Budget {
        captor.capture()
        return Budget(
            id = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            budgetMonth = BUDGET_MONTH,
            plannedIncome = BigDecimal.ZERO,
            savingsTarget = BigDecimal.ZERO,
            currency = Currency.RUB,
            createdAt = OffsetDateTime.now(clock),
            updatedAt = OffsetDateTime.now(clock),
        )
    }
}
