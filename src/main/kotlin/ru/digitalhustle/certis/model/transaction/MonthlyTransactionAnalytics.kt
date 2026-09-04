package ru.digitalhustle.certis.model.transaction

import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.YearMonth

data class MonthlyTransactionAnalyticsFilter(

    val month: YearMonth,

    val currency: Currency,
)

data class MonthlyTransactionAnalytics(

    val month: YearMonth,

    val currency: Currency,

    val income: MonthlyTransactionTotal,

    val expenses: MonthlyTransactionTotal,

    val netCashFlow: BigDecimal,
)

data class MonthlyTransactionTotal(

    val transactionCount: Int,

    val amount: BigDecimal,
)
