package ru.digitalhustle.certis.features.transaction.query.model

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.transaction.enums.CashFlowGranularity
import ru.digitalhustle.certis.features.transaction.enums.CashFlowRange
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

data class CashFlowAnalyticsFilter(

    val range: CashFlowRange,

    val currency: Currency,

    val anchorDate: LocalDate,

    val timeZone: ZoneId,
)

data class CashFlowAnalytics(

    val range: CashFlowRange,

    val currency: Currency,

    val granularity: CashFlowGranularity,

    val from: OffsetDateTime,

    val toExclusive: OffsetDateTime,

    val totals: CashFlowTotals,

    val points: List<CashFlowPoint>,
)

data class CashFlowTotals(

    val income: BigDecimal,

    val expenses: BigDecimal,

    val netCashFlow: BigDecimal,
)

data class CashFlowPoint(

    val bucketStart: OffsetDateTime,

    val income: BigDecimal,

    val expenses: BigDecimal,

    val netCashFlow: BigDecimal,
)
