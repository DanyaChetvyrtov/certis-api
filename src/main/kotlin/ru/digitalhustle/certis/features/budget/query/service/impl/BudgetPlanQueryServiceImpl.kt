package ru.digitalhustle.certis.features.budget.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.budget.exceptions.BudgetPlanNotFoundException
import ru.digitalhustle.certis.features.budget.model.BudgetPlan
import ru.digitalhustle.certis.features.budget.model.BudgetPlanRevisions
import ru.digitalhustle.certis.features.budget.model.BudgetPlanView
import ru.digitalhustle.certis.features.budget.query.repository.BudgetPlanQueryRepository
import ru.digitalhustle.certis.features.budget.query.service.BudgetPlanQueryService
import ru.digitalhustle.certis.features.budget.util.BudgetPlanViewFactory
import ru.digitalhustle.certis.shared.enums.Currency
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BudgetPlanQueryServiceImpl(
    private val repository: BudgetPlanQueryRepository,
    private val viewFactory: BudgetPlanViewFactory,
) : BudgetPlanQueryService {

    override fun getEntityById(id: UUID, userId: UUID): BudgetPlan =
        repository.findByIdAndUserId(id, userId)
            ?: throw BudgetPlanNotFoundException(details = mapOf("planId" to id))

    override fun getCurrent(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ): BudgetPlanView =
        repository.findCurrentByUserIdAndScope(userId, budgetMonth, currency)
            ?.let(::toView)
            ?: throw BudgetPlanNotFoundException(
                details = mapOf(
                    "month" to YearMonth.from(budgetMonth).toString(),
                    "currency" to currency.name,
                ),
            )

    override fun getById(
        id: UUID,
        userId: UUID,
    ): BudgetPlanView =
        toView(getEntityById(id, userId))

    override fun getRevisions(
        userId: UUID,
        budgetMonth: LocalDate,
        currency: Currency,
    ): BudgetPlanRevisions {
        val plans = repository.findAllByUserIdAndScope(userId, budgetMonth, currency)
        val states = repository.findStatesByPlanIds(plans.map(BudgetPlan::id))

        return BudgetPlanRevisions(
            items = plans.map { plan ->
                viewFactory.createRevision(
                    plan = plan,
                    state = requireNotNull(states[plan.id]),
                )
            },
        )
    }

    private fun toView(plan: BudgetPlan): BudgetPlanView =
        viewFactory.create(
            plan = plan,
            state = requireNotNull(repository.findStatesByPlanIds(listOf(plan.id))[plan.id]),
        )
}
