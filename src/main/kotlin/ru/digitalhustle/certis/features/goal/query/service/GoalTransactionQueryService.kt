package ru.digitalhustle.certis.features.goal.query.service

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.goal.model.GoalContributionFilter
import ru.digitalhustle.certis.features.goal.model.GoalContributionPage
import ru.digitalhustle.certis.features.goal.model.GoalMonthlyAmount
import java.time.OffsetDateTime
import java.util.UUID

interface GoalTransactionQueryService {

    fun getPage(goalId: UUID, userId: UUID, filter: GoalContributionFilter): GoalContributionPage

    fun getNetAmounts(
        userId: UUID,
        currency: Currency,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<GoalMonthlyAmount>
}
