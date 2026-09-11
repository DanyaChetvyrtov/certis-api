package ru.digitalhustle.certis.features.budget.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.budget.model.BudgetDetails
import ru.digitalhustle.certis.features.budget.query.repository.BudgetQueryRepository
import ru.digitalhustle.certis.features.budget.query.service.BudgetQueryService
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BudgetQueryServiceImpl(
    private val budgetRepository: BudgetQueryRepository,
    private val applicationClock: ApplicationClock,
) : BudgetQueryService {

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getByMonth(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetDetails = getDetails(userId, budgetMonth)

    private fun getDetails(
        userId: UUID,
        budgetMonth: LocalDate,
    ): BudgetDetails {
        val month = YearMonth.from(budgetMonth)
        val monthStart = applicationClock.startOfMonth(month)
        val nextMonthStart = applicationClock.startOfNextMonth(month)

        return budgetRepository.findDetailsByUserIdAndMonth(
            userId = userId,
            budgetMonth = budgetMonth,
            monthStart = monthStart,
            nextMonthStart = nextMonthStart,
        ) ?: throw NotFoundException.entity("Budget")
    }
}
