package ru.digitalhustle.certis.features.transaction.query.service

import ru.digitalhustle.certis.features.transaction.query.model.CashFlowAnalytics
import ru.digitalhustle.certis.features.transaction.query.model.CashFlowAnalyticsFilter
import ru.digitalhustle.certis.features.transaction.query.model.MonthlyTransactionAnalytics
import ru.digitalhustle.certis.features.transaction.query.model.MonthlyTransactionAnalyticsFilter
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
