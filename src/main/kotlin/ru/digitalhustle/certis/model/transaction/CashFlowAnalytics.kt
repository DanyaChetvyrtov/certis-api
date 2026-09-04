package ru.digitalhustle.certis.model.transaction

import ru.digitalhustle.certis.enums.CashFlowGranularity
import ru.digitalhustle.certis.enums.CashFlowRange
import ru.digitalhustle.certis.enums.Currency
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
