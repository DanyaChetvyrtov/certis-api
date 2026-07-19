package ru.digitalhustle.certis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import ru.digitalhustle.certis.config.properties.AppApiProperties
import ru.digitalhustle.certis.config.properties.AppMinioProperties
import ru.digitalhustle.certis.config.properties.AuthRateLimitProperties
import ru.digitalhustle.certis.config.properties.ClockProperties
import ru.digitalhustle.certis.config.properties.JwtProperties
import ru.digitalhustle.certis.config.properties.RecurringTransactionProperties

@EnableWebSecurity
@EnableMethodSecurity
@EnableScheduling
@EnableConfigurationProperties(
    JwtProperties::class,
    AppApiProperties::class,
    AppMinioProperties::class,
    AuthRateLimitProperties::class,
    ClockProperties::class,
    RecurringTransactionProperties::class,
)
@SpringBootApplication
class CertisApplication

fun main(args: Array<String>) {
    runApplication<CertisApplication>(*args)
}
