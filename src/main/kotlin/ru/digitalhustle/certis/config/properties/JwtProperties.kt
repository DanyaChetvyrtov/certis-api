package ru.digitalhustle.certis.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "digital-hustle.certis.security.jwt")
data class JwtProperties(

    val secret: String,

    val accessDuration: Duration,

    val refreshDuration: Duration,
)
