package ru.digitalhustle.certis.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.digitalhustle.certis.config.properties.ClockProperties
import java.time.Clock

@Configuration
class ClockConfig(
    private val properties: ClockProperties,
) {

    @Bean
    fun clock(): Clock = Clock.system(properties.zone)
}
