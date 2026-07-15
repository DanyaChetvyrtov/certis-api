package ru.digitalhustle.certis.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@Configuration
class TimeConfig {

    @Bean
    fun clock(): Clock {
        val zoneId = ZoneId.of("UTC")

        return Clock.system(zoneId)
    }
}
