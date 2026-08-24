package ru.digitalhustle.certis.units.config

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import ru.digitalhustle.certis.config.ClockConfig
import ru.digitalhustle.certis.config.properties.ClockProperties
import java.time.ZoneId

class ClockConfigTest {

    @Test
    fun `should create clock with configured time zone`() {
        // given
        val properties = ClockProperties(zoneId = "Europe/Riga")

        // when
        val clock = ClockConfig(properties).clock()

        // then
        assertThat(clock.zone).isEqualTo(ZoneId.of("Europe/Riga"))
    }

    @Test
    fun `should reject invalid time zone`() {
        assertThatThrownBy {
            ClockProperties(zoneId = "Invalid/TimeZone")
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Application time zone must be a valid zone ID")
    }
}
