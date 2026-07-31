package ru.digitalhustle.certis.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "digital-hustle.certis.api")
data class AppApiProperties(

    val publicUrl: String,
)
