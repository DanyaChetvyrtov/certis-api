package ru.digitalhustle.certis.util.time

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId

@Component
class ApplicationClock(
    private val clock: Clock,
) {

    fun now(): OffsetDateTime = OffsetDateTime.now(clock)

    fun instant(): Instant = clock.instant()

    fun today(): LocalDate = LocalDate.now(clock)

    fun currentMonth(): YearMonth = YearMonth.now(clock)

    fun zoneId(): ZoneId = clock.zone

    fun startOfDay(date: LocalDate): OffsetDateTime =
        date.atStartOfDay(clock.zone).toOffsetDateTime()

    fun startOfMonth(month: YearMonth): OffsetDateTime =
        startOfDay(month.atDay(1))

    fun startOfNextMonth(month: YearMonth): OffsetDateTime =
        startOfMonth(month.plusMonths(1))
}
