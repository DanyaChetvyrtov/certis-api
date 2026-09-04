package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.MoneyConstants
import ru.digitalhustle.certis.enums.CashFlowGranularity
import ru.digitalhustle.certis.enums.CashFlowRange
import ru.digitalhustle.certis.model.transaction.CashFlowAnalytics
import ru.digitalhustle.certis.model.transaction.CashFlowAnalyticsFilter
import ru.digitalhustle.certis.model.transaction.CashFlowPoint
import ru.digitalhustle.certis.model.transaction.CashFlowTotals
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionAnalytics
import ru.digitalhustle.certis.model.transaction.MonthlyTransactionAnalyticsFilter
import ru.digitalhustle.certis.repository.TransactionAnalyticsRepository
import ru.digitalhustle.certis.service.domain.TransactionAnalyticsService
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Service
class TransactionAnalyticsServiceImpl(
    private val transactionAnalyticsRepository: TransactionAnalyticsRepository,
    private val clock: Clock,
) : TransactionAnalyticsService {

    @Transactional(readOnly = true)
    override fun getMonthlyAnalytics(
        userId: UUID,
        filter: MonthlyTransactionAnalyticsFilter,
    ): MonthlyTransactionAnalytics {
        val monthStart = filter.month.atDay(1).atStartOfDay(clock.zone).toOffsetDateTime()
        val nextMonthStart = filter.month.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toOffsetDateTime()

        return transactionAnalyticsRepository.findMonthlyByUserId(
            userId = userId,
            filter = filter,
            monthStart = monthStart,
            nextMonthStart = nextMonthStart,
        )
    }

    @Transactional(readOnly = true)
    override fun getCashFlowAnalytics(
        userId: UUID,
        filter: CashFlowAnalyticsFilter,
    ): CashFlowAnalytics {
        val period = cashFlowPeriod(filter)
        val existingPoints = transactionAnalyticsRepository.findCashFlowPointsByUserId(
            userId = userId,
            filter = filter,
            granularity = period.granularity,
            from = period.from.toOffsetDateTime(),
            toExclusive = period.toExclusive.toOffsetDateTime(),
        ).associateBy { point -> point.bucketStart.toInstant() }
        val points = generatePoints(period, existingPoints)
        val totalIncome = points.fold(ZERO_AMOUNT) { total, point -> total + point.income }
        val totalExpenses = points.fold(ZERO_AMOUNT) { total, point -> total + point.expenses }

        return CashFlowAnalytics(
            range = filter.range,
            currency = filter.currency,
            granularity = period.granularity,
            from = period.from.toOffsetDateTime(),
            toExclusive = period.toExclusive.toOffsetDateTime(),
            totals = CashFlowTotals(
                income = totalIncome,
                expenses = totalExpenses,
                netCashFlow = totalIncome - totalExpenses,
            ),
            points = points,
        )
    }

    private fun cashFlowPeriod(filter: CashFlowAnalyticsFilter): CashFlowPeriod {
        val startDate: LocalDate
        val endDate: LocalDate
        val granularity: CashFlowGranularity

        when (filter.range) {
            CashFlowRange.DAY -> {
                startDate = filter.anchorDate
                endDate = startDate.plusDays(1)
                granularity = CashFlowGranularity.HOUR
            }

            CashFlowRange.WEEK -> {
                startDate = filter.anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                endDate = startDate.plusWeeks(1)
                granularity = CashFlowGranularity.DAY
            }

            CashFlowRange.MONTH -> {
                startDate = filter.anchorDate.withDayOfMonth(1)
                endDate = startDate.plusMonths(1)
                granularity = CashFlowGranularity.DAY
            }

            CashFlowRange.SIX_MONTHS -> {
                startDate = filter.anchorDate.withDayOfMonth(1).minusMonths(SIX_MONTHS_OFFSET)
                endDate = filter.anchorDate.withDayOfMonth(1).plusMonths(1)
                granularity = CashFlowGranularity.MONTH
            }

            CashFlowRange.YEAR -> {
                startDate = filter.anchorDate.withDayOfMonth(1).minusMonths(YEAR_OFFSET)
                endDate = filter.anchorDate.withDayOfMonth(1).plusMonths(1)
                granularity = CashFlowGranularity.MONTH
            }
        }

        return CashFlowPeriod(
            granularity = granularity,
            from = startDate.atStartOfDay(filter.timeZone),
            toExclusive = endDate.atStartOfDay(filter.timeZone),
        )
    }

    private fun generatePoints(
        period: CashFlowPeriod,
        existingPoints: Map<Instant, CashFlowPoint>,
    ): List<CashFlowPoint> =
        generateSequence(period.from) { current -> current.next(period.granularity) }
            .takeWhile { current -> current.isBefore(period.toExclusive) }
            .map { bucketStart ->
                existingPoints[bucketStart.toInstant()]
                    ?.copy(bucketStart = bucketStart.toOffsetDateTime())
                    ?: CashFlowPoint(
                        bucketStart = bucketStart.toOffsetDateTime(),
                        income = ZERO_AMOUNT,
                        expenses = ZERO_AMOUNT,
                        netCashFlow = ZERO_AMOUNT,
                    )
            }
            .toList()

    private fun ZonedDateTime.next(granularity: CashFlowGranularity): ZonedDateTime =
        when (granularity) {
            CashFlowGranularity.HOUR -> plusHours(1)
            CashFlowGranularity.DAY -> plusDays(1)
            CashFlowGranularity.MONTH -> plusMonths(1)
        }

    private data class CashFlowPeriod(
        val granularity: CashFlowGranularity,
        val from: ZonedDateTime,
        val toExclusive: ZonedDateTime,
    )

    private companion object {
        const val SIX_MONTHS_OFFSET = 5L
        const val YEAR_OFFSET = 11L
        val ZERO_AMOUNT: BigDecimal = BigDecimal.ZERO.setScale(MoneyConstants.MONEY_SCALE)
    }
}
