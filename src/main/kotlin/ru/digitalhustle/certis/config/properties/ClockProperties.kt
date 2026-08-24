package ru.digitalhustle.certis.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.DateTimeException
import java.time.ZoneId

@ConfigurationProperties(prefix = "digital-hustle.certis.time")
data class ClockProperties(

    val zoneId: String,
) {

    val zone: ZoneId = try {
        ZoneId.of(zoneId)
    } catch (exception: DateTimeException) {
        throw IllegalArgumentException("Application time zone must be a valid zone ID", exception)
    }
}
