package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.enums.CashFlowGranularity
import ru.digitalhustle.certis.enums.CashFlowRange
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.model.transaction.CashFlowAnalyticsFilter
import ru.digitalhustle.certis.model.transaction.CashFlowPoint
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionAnalytics
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionAnalyticsFilter
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionTotal
import ru.digitalhustle.certis.repository.TransactionAnalyticsRepository
import ru.digitalhustle.certis.service.domain.impl.TransactionAnalyticsServiceImpl
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class TransactionAnalyticsServiceImplTest {

    private val transactionAnalyticsRepository = mock(TransactionAnalyticsRepository::class.java)
    private val clock = Clock.fixed(
        Instant.parse("2026-09-15T10:15:30Z"),
        ZoneId.of("Europe/Riga"),
    )
    private val transactionAnalyticsService =
        TransactionAnalyticsServiceImpl(transactionAnalyticsRepository, clock)

    private companion object {
        private val MONTH = YearMonth.of(2026, 9)
        private val MONTH_START = OffsetDateTime.parse("2026-09-01T00:00:00+03:00")
        private val NEXT_MONTH_START = OffsetDateTime.parse("2026-10-01T00:00:00+03:00")
        private val CASH_FLOW_ANCHOR_DATE = LocalDate.of(2026, 9, 4)
        private val CASH_FLOW_TIME_ZONE = ZoneId.of("Europe/Moscow")
        private val EXPECTED_CASH_FLOW_RANGES = listOf(
            ExpectedCashFlowRange(
                range = CashFlowRange.DAY,
                granularity = CashFlowGranularity.HOUR,
                from = "2026-09-04T00:00:00+03:00",
                toExclusive = "2026-09-05T00:00:00+03:00",
                pointCount = 24,
            ),
            ExpectedCashFlowRange(
                range = CashFlowRange.WEEK,
                granularity = CashFlowGranularity.DAY,
                from = "2026-08-31T00:00:00+03:00",
                toExclusive = "2026-09-07T00:00:00+03:00",
                pointCount = 7,
            ),
            ExpectedCashFlowRange(
                range = CashFlowRange.MONTH,
                granularity = CashFlowGranularity.DAY,
                from = "2026-09-01T00:00:00+03:00",
                toExclusive = "2026-10-01T00:00:00+03:00",
                pointCount = 30,
            ),
            ExpectedCashFlowRange(
                range = CashFlowRange.SIX_MONTHS,
                granularity = CashFlowGranularity.MONTH,
                from = "2026-04-01T00:00:00+03:00",
                toExclusive = "2026-10-01T00:00:00+03:00",
                pointCount = 6,
            ),
            ExpectedCashFlowRange(
                range = CashFlowRange.YEAR,
                granularity = CashFlowGranularity.MONTH,
                from = "2025-10-01T00:00:00+03:00",
                toExclusive = "2026-10-01T00:00:00+03:00",
                pointCount = 12,
            ),
        )
    }

    @Test
    fun `should get monthly analytics using application time zone boundaries`() {
        // given
        val userId = UUID.randomUUID()
        val filter = MonthlyTransactionAnalyticsFilter(MONTH, Currency.RUB)
        val analytics = MonthlyTransactionAnalytics(
            month = MONTH,
            currency = Currency.RUB,
            income = MonthlyTransactionTotal(0, BigDecimal("0.00")),
            expenses = MonthlyTransactionTotal(0, BigDecimal("0.00")),
            netCashFlow = BigDecimal("0.00"),
        )

        `when`(
            transactionAnalyticsRepository.findMonthlyByUserId(
                userId,
                filter,
                MONTH_START,
                NEXT_MONTH_START,
            ),
        ).thenReturn(analytics)

        // when
        val result = transactionAnalyticsService.getMonthlyAnalytics(userId, filter)

        // then
        assertThat(result).isEqualTo(analytics)
    }

    @Test
    fun `should resolve cash flow periods and granularities`() {
        // given
        val userId = UUID.randomUUID()

        EXPECTED_CASH_FLOW_RANGES.forEach { expected ->
            val filter = cashFlowFilter(expected.range)
            `when`(
                transactionAnalyticsRepository.findCashFlowPointsByUserId(
                    userId = userId,
                    filter = filter,
                    granularity = expected.granularity,
                    from = OffsetDateTime.parse(expected.from),
                    toExclusive = OffsetDateTime.parse(expected.toExclusive),
                ),
            ).thenReturn(emptyList())

            // when
            val result = transactionAnalyticsService.getCashFlowAnalytics(userId, filter)

            // then
            assertThat(result.granularity).isEqualTo(expected.granularity)
            assertThat(result.from).isEqualTo(OffsetDateTime.parse(expected.from))
            assertThat(result.toExclusive).isEqualTo(OffsetDateTime.parse(expected.toExclusive))
            assertThat(result.points).hasSize(expected.pointCount)
            assertThat(result.points.map { point -> point.bucketStart }).isSorted()
            result.points.forEach { point ->
                assertThat(point.income).isEqualByComparingTo("0.00")
                assertThat(point.expenses).isEqualByComparingTo("0.00")
                assertThat(point.netCashFlow).isEqualByComparingTo("0.00")
            }
        }
    }

    @Test
    fun `should fill missing cash flow buckets and calculate totals`() {
        // given
        val userId = UUID.randomUUID()
        val filter = cashFlowFilter(CashFlowRange.SIX_MONTHS)
        val aprilPoint = cashFlowPoint("2026-04-01T00:00:00+03:00", "1000.00", "100.00")
        val septemberPoint = cashFlowPoint("2026-09-01T00:00:00+03:00", "500.00", "250.50")
        `when`(
            transactionAnalyticsRepository.findCashFlowPointsByUserId(
                userId = userId,
                filter = filter,
                granularity = CashFlowGranularity.MONTH,
                from = OffsetDateTime.parse("2026-04-01T00:00:00+03:00"),
                toExclusive = OffsetDateTime.parse("2026-10-01T00:00:00+03:00"),
            ),
        ).thenReturn(listOf(aprilPoint, septemberPoint))

        // when
        val result = transactionAnalyticsService.getCashFlowAnalytics(userId, filter)

        // then
        assertThat(result.points).hasSize(6)
        assertThat(result.points[0]).isEqualTo(aprilPoint)
        assertThat(result.points[1].income).isEqualByComparingTo("0.00")
        assertThat(result.points[5]).isEqualTo(septemberPoint)
        assertThat(result.totals.income).isEqualByComparingTo("1500.00")
        assertThat(result.totals.expenses).isEqualByComparingTo("350.50")
        assertThat(result.totals.netCashFlow).isEqualByComparingTo("1149.50")
    }

    private fun cashFlowFilter(range: CashFlowRange): CashFlowAnalyticsFilter =
        CashFlowAnalyticsFilter(
            range = range,
            currency = Currency.RUB,
            anchorDate = CASH_FLOW_ANCHOR_DATE,
            timeZone = CASH_FLOW_TIME_ZONE,
        )

    private fun cashFlowPoint(
        bucketStart: String,
        income: String,
        expenses: String,
    ): CashFlowPoint =
        CashFlowPoint(
            bucketStart = OffsetDateTime.parse(bucketStart),
            income = BigDecimal(income),
            expenses = BigDecimal(expenses),
            netCashFlow = BigDecimal(income) - BigDecimal(expenses),
        )

    private data class ExpectedCashFlowRange(
        val range: CashFlowRange,
        val granularity: CashFlowGranularity,
        val from: String,
        val toExclusive: String,
        val pointCount: Int,
    )
}
