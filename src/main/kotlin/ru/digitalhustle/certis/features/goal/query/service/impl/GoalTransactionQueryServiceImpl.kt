package ru.digitalhustle.certis.features.goal.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.goal.model.GoalContributionFilter
import ru.digitalhustle.certis.features.goal.model.GoalContributionPage
import ru.digitalhustle.certis.features.goal.model.GoalMonthlyAmount
import ru.digitalhustle.certis.features.goal.query.repository.GoalTransactionQueryRepository
import ru.digitalhustle.certis.features.goal.query.service.GoalTransactionQueryService
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class GoalTransactionQueryServiceImpl(
    private val goalTransactionRepository: GoalTransactionQueryRepository,
) : GoalTransactionQueryService {

    override fun getPage(
        goalId: UUID,
        userId: UUID,
        filter: GoalContributionFilter,
    ): GoalContributionPage =
        GoalContributionPage(
            items = goalTransactionRepository.findPageByGoalIdAndUserId(goalId, userId, filter),
            page = filter.page,
            size = filter.size,
            totalElements = goalTransactionRepository.countByGoalIdAndUserId(goalId, userId),
        )

    override fun getNetAmounts(
        userId: UUID,
        currency: Currency,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<GoalMonthlyAmount> = goalTransactionRepository.findNetAmountsByGoalId(userId, currency, from, to)
}
