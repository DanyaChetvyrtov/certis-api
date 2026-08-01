package ru.digitalhustle.certis.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "digital-hustle.certis.security.rate-limit")
data class AuthRateLimitProperties(

    val cacheMaximumSize: Long,

    val cacheExpireAfter: Duration,

    val login: Rule,

    val registration: Rule,

    val refresh: Rule,
) {

    init {
        require(cacheMaximumSize > 0) { "Rate limit cache maximum size must be positive" }
        require(!cacheExpireAfter.isNegative && !cacheExpireAfter.isZero) {
            "Rate limit cache expiration must be positive"
        }
    }

    data class Rule(
        val capacity: Long,
        val refillPeriod: Duration,
    ) {

        init {
            require(capacity > 0) { "Rate limit capacity must be positive" }
            require(!refillPeriod.isNegative && !refillPeriod.isZero) {
                "Rate limit refill period must be positive"
            }
        }
    }
}
