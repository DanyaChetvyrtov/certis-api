package ru.digitalhustle.certis.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "digital-hustle.certis.security.jwt")
data class JwtProperties(

    var secret: String,

    val accessDuration: Long,

    val refreshDuration: Long
)
