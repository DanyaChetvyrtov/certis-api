package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.transaction.CashFlowAnalytics
import ru.digitalhustle.certis.model.transaction.CashFlowAnalyticsFilter
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionAnalytics
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionAnalyticsFilter
import java.util.UUID

interface TransactionAnalyticsService {

    fun getMonthlyAnalytics(
        userId: UUID,
        filter: MonthlyTransactionAnalyticsFilter,
    ): MonthlyTransactionAnalytics

    fun getCashFlowAnalytics(
        userId: UUID,
        filter: CashFlowAnalyticsFilter,
    ): CashFlowAnalytics
}
