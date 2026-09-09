package ru.digitalhustle.certis.units.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import ru.digitalhustle.certis.time.ApplicationClock
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId

class ApplicationClockTest {

    private val zone = ZoneId.of("Europe/Riga")
    private val clock = Clock.fixed(Instant.parse("2026-03-29T00:30:00Z"), zone)
    private val applicationClock = ApplicationClock(clock)

    @Test
    fun `should expose current application time`() {
        // when
        val now = applicationClock.now()

        // then
        assertAll(
            { assertThat(now).isEqualTo(OffsetDateTime.parse("2026-03-29T02:30:00+02:00")) },
            { assertThat(applicationClock.instant()).isEqualTo(clock.instant()) },
            { assertThat(applicationClock.today()).isEqualTo(LocalDate.of(2026, 3, 29)) },
            { assertThat(applicationClock.currentMonth()).isEqualTo(YearMonth.of(2026, 3)) },
        )
    }

    @Test
    fun `should calculate start of day in application zone`() {
        // given
        val date = LocalDate.of(2026, 3, 29)

        // when
        val result = applicationClock.startOfDay(date)

        // then
        assertThat(result).isEqualTo(OffsetDateTime.parse("2026-03-29T00:00:00+02:00"))
    }

    @Test
    fun `should preserve regional offsets across month boundaries`() {
        // given
        val month = YearMonth.of(2026, 3)

        // when
        val monthStart = applicationClock.startOfMonth(month)
        val nextMonthStart = applicationClock.startOfNextMonth(month)

        // then
        assertAll(
            { assertThat(monthStart).isEqualTo(OffsetDateTime.parse("2026-03-01T00:00:00+02:00")) },
            { assertThat(nextMonthStart).isEqualTo(OffsetDateTime.parse("2026-04-01T00:00:00+03:00")) },
        )
    }
}
