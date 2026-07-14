package ru.digitalhustle.certis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import ru.digitalhustle.certis.config.properties.AppApiProperties
import ru.digitalhustle.certis.config.properties.AppMinioProperties
import ru.digitalhustle.certis.config.properties.JwtProperties

@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(
    JwtProperties::class,
    AppApiProperties::class,
    AppMinioProperties::class,
)
@SpringBootApplication
class CertisApplication

fun main(args: Array<String>) {
    runApplication<CertisApplication>(*args)
}
