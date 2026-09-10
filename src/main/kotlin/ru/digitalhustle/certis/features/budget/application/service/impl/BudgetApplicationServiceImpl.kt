package ru.digitalhustle.certis.features.budget.application.service.impl

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.budget.application.service.BudgetApplicationService
import ru.digitalhustle.certis.features.budget.application.validator.BudgetValidator
import ru.digitalhustle.certis.features.budget.command.model.ApplyBudgetOptimizationData
import ru.digitalhustle.certis.features.budget.command.model.SaveBudgetData
import ru.digitalhustle.certis.features.budget.command.service.BudgetAllocationService
import ru.digitalhustle.certis.features.budget.command.service.BudgetCommandStore
import ru.digitalhustle.certis.features.budget.exceptions.InvalidBudgetException
import ru.digitalhustle.certis.features.budget.model.Budget
import ru.digitalhustle.certis.features.budget.model.BudgetCategory
import ru.digitalhustle.certis.features.budget.model.BudgetDetails
import ru.digitalhustle.certis.features.budget.query.service.BudgetQueryService
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.security.api.UserPreferencesCommand
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BudgetApplicationServiceImpl(
    private val budgetStore: BudgetCommandStore,
    private val budgetAllocationService: BudgetAllocationService,
    private val budgetQueryService: BudgetQueryService,
    private val userPreferencesCommand: UserPreferencesCommand,
    private val budgetValidator: BudgetValidator,
    private val applicationClock: ApplicationClock,
) : BudgetApplicationService {

    @Transactional
    override fun getByMonthForUpdate(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetDetails {
        budgetStore.findByUserIdAndMonthForUpdate(userId, budgetMonth)
            ?: throw NotFoundException.entity("Budget")

        return budgetQueryService.getByMonth(userId, budgetMonth)
    }

    @Transactional
    override fun save(budget: SaveBudgetData): BudgetDetails {
        budgetValidator.validateBudget(budget)

        val preferredCurrency = userPreferencesCommand.findPreferredCurrencyForUpdate(budget.userId)
            ?: throw NotFoundException.entity("User")
        budgetValidator.validateCategories(budget)

        return translateConstraintViolation {
            val currentBudget = budgetStore.findByUserIdAndMonthForUpdate(
                userId = budget.userId,
                budgetMonth = budget.budgetMonth,
            )
            val now = applicationClock.now()

            currentBudget?.let { budgetAllocationService.deleteAllocations(it.id) }

            val savedBudget = if (currentBudget == null) {
                budgetStore.insert(budget.toEntity(now, preferredCurrency))
            } else {
                budgetStore.update(currentBudget.updateFrom(budget, now))
            }

            budgetAllocationService.insertAllocations(
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

            budgetQueryService.getByMonth(budget.userId, budget.budgetMonth)
        }
    }

    @Transactional
    override fun applyOptimization(data: ApplyBudgetOptimizationData): BudgetDetails {
        val currentBudget = budgetStore.findByUserIdAndMonthForUpdate(
            userId = data.userId,
            budgetMonth = data.inputSnapshot.budgetMonth,
        ) ?: throw NotFoundException.entity("Budget")
        val currentDetails = budgetQueryService.getByMonth(data.userId, data.inputSnapshot.budgetMonth)

        budgetValidator.validateOptimization(data, currentDetails)

        val optimizedBudget = SaveBudgetData(
            userId = data.userId,
            budgetMonth = currentDetails.budgetMonth,
            plannedIncome = currentDetails.plannedIncome,
            savingsTarget = currentDetails.savingsTarget,
            allocations = data.allocations,
        )
        budgetValidator.validateBudget(optimizedBudget)
        budgetValidator.validateCategories(optimizedBudget)

        return translateConstraintViolation {
            budgetAllocationService.deleteAllocations(currentBudget.id)
            budgetStore.update(currentBudget.copy(updatedAt = applicationClock.now()))
            budgetAllocationService.insertAllocations(
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

            budgetQueryService.getByMonth(data.userId, currentDetails.budgetMonth)
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
