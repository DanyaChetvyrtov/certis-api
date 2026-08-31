package ru.digitalhustle.certis.service.domain.impl

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.BudgetOptimizationConflictException
import ru.digitalhustle.certis.exception.custom.InvalidBudgetException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.budget.ApplyBudgetOptimizationData
import ru.digitalhustle.certis.model.budget.BudgetDetails
import ru.digitalhustle.certis.model.budget.SaveBudgetData
import ru.digitalhustle.certis.model.entity.Budget
import ru.digitalhustle.certis.model.entity.BudgetCategory
import ru.digitalhustle.certis.repository.BudgetRepository
import ru.digitalhustle.certis.service.domain.BudgetService
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BudgetServiceImpl(
    private val budgetRepository: BudgetRepository,
    private val clock: Clock,
) : BudgetService {

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getByMonth(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetDetails = getDetails(userId, budgetMonth)

    @Transactional
    override fun getByMonthForUpdate(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetDetails {
        budgetRepository.findByUserIdAndMonthForUpdate(userId, budgetMonth)
            ?: throw NotFoundException.entity("Budget")

        return getDetails(userId, budgetMonth)
    }

    @Transactional
    override fun save(budget: SaveBudgetData): BudgetDetails {
        validateBudget(budget)

        val preferredCurrency = budgetRepository.findPreferredCurrencyByUserIdForUpdate(budget.userId)
            ?: throw NotFoundException.entity("User")
        validateCategories(budget)

        return translateConstraintViolation {
            val currentBudget = budgetRepository.findByUserIdAndMonthForUpdate(
                userId = budget.userId,
                budgetMonth = budget.budgetMonth,
            )
            val now = OffsetDateTime.now(clock)

            currentBudget?.let { budgetRepository.deleteAllocations(it.id) }

            val savedBudget = if (currentBudget == null) {
                budgetRepository.insert(budget.toEntity(now, preferredCurrency))
            } else {
                budgetRepository.update(currentBudget.updateFrom(budget, now))
            }

            budgetRepository.insertAllocations(
                budget.allocations.map { allocation ->
                    BudgetCategory(
                        id = UUID.randomUUID(),
                        userId = budget.userId,
                        budgetId = savedBudget.id,
                        categoryId = allocation.categoryId,
                        categoryType = CategoryType.EXPENSE,
                        limitAmount = allocation.limitAmount,
                        expenseType = allocation.expenseType,
                    )
                },
            )

            getDetails(budget.userId, budget.budgetMonth)
        }
    }

    @Transactional
    override fun applyOptimization(data: ApplyBudgetOptimizationData): BudgetDetails {
        val currentBudget = budgetRepository.findByUserIdAndMonthForUpdate(
            userId = data.userId,
            budgetMonth = data.inputSnapshot.budgetMonth,
        ) ?: throw NotFoundException.entity("Budget")
        val currentDetails = getDetails(data.userId, data.inputSnapshot.budgetMonth)

        if (!data.inputSnapshot.matches(currentDetails) || !data.matchesSnapshotAllocations()) {
            throw BudgetOptimizationConflictException(ErrorMessages.BUDGET_OPTIMIZATION_STALE)
        }

        val optimizedBudget = SaveBudgetData(
            userId = data.userId,
            budgetMonth = currentDetails.budgetMonth,
            plannedIncome = currentDetails.plannedIncome,
            savingsTarget = currentDetails.savingsTarget,
            allocations = data.allocations,
        )
        validateBudget(optimizedBudget)
        validateCategories(optimizedBudget)

        return translateConstraintViolation {
            budgetRepository.deleteAllocations(currentBudget.id)
            budgetRepository.update(currentBudget.copy(updatedAt = OffsetDateTime.now(clock)))
            budgetRepository.insertAllocations(
                data.allocations.map { allocation ->
                    BudgetCategory(
                        id = UUID.randomUUID(),
                        userId = data.userId,
                        budgetId = currentBudget.id,
                        categoryId = allocation.categoryId,
                        categoryType = CategoryType.EXPENSE,
                        limitAmount = allocation.limitAmount,
                        expenseType = allocation.expenseType,
                    )
                },
            )

            getDetails(data.userId, currentDetails.budgetMonth)
        }
    }

    private fun getDetails(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetDetails {
        val monthStart = budgetMonth.atStartOfDay(clock.zone).toOffsetDateTime()
        val nextMonthStart = budgetMonth.plusMonths(1).atStartOfDay(clock.zone).toOffsetDateTime()

        return budgetRepository.findDetailsByUserIdAndMonth(
            userId = userId,
            budgetMonth = budgetMonth,
            monthStart = monthStart,
            nextMonthStart = nextMonthStart,
        ) ?: throw NotFoundException.entity("Budget")
    }

    private fun validateBudget(budget: SaveBudgetData) {
        val categoryIds = budget.allocations.map { allocation -> allocation.categoryId }

        if (categoryIds.distinct().size != categoryIds.size) {
            throw InvalidBudgetException(ErrorMessages.BUDGET_DUPLICATE_CATEGORIES)
        }

        val allocatedAmount = budget.allocations.fold(BigDecimal.ZERO) { total, allocation ->
            total + allocation.limitAmount
        }
        if (allocatedAmount + budget.savingsTarget > budget.plannedIncome) {
            throw InvalidBudgetException(ErrorMessages.BUDGET_ALLOCATIONS_EXCEED_INCOME)
        }
    }

    private fun validateCategories(budget: SaveBudgetData) {
        val categoryIds = budget.allocations.map { allocation -> allocation.categoryId }
        val activeExpenseCategories = budgetRepository.countActiveExpenseCategories(
            userId = budget.userId,
            categoryIds = categoryIds,
        )

        if (activeExpenseCategories != categoryIds.size) {
            throw InvalidBudgetException(ErrorMessages.BUDGET_CATEGORY_INVALID)
        }
    }

    private fun ApplyBudgetOptimizationData.matchesSnapshotAllocations(): Boolean {
        if (allocations.size != inputSnapshot.allocations.size) {
            return false
        }

        val sourceByCategory = inputSnapshot.allocations.associateBy { allocation -> allocation.categoryId }

        return allocations.all { allocation ->
            sourceByCategory[allocation.categoryId]?.expenseType == allocation.expenseType
        }
    }

    private fun <T> translateConstraintViolation(action: () -> T): T =
        try {
            action()
        } catch (exception: DataIntegrityViolationException) {
            throw InvalidBudgetException(ErrorMessages.BUDGET_CONSTRAINT_VIOLATION, exception)
        }

    private fun SaveBudgetData.toEntity(
        now: OffsetDateTime,
        preferredCurrency: Currency,
    ): Budget =
        Budget(
            id = UUID.randomUUID(),
            userId = userId,
            budgetMonth = budgetMonth,
            plannedIncome = plannedIncome,
            savingsTarget = savingsTarget,
            currency = preferredCurrency,
            createdAt = now,
            updatedAt = now,
        )

    private fun Budget.updateFrom(
        source: SaveBudgetData,
        now: OffsetDateTime,
    ): Budget =
        copy(
            plannedIncome = source.plannedIncome,
            savingsTarget = source.savingsTarget,
            updatedAt = now,
        )
}
