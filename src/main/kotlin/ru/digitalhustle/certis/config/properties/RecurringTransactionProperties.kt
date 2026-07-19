package ru.digitalhustle.certis.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "digital-hustle.certis.recurring-transactions")
data class RecurringTransactionProperties(
    val scheduler: Scheduler,
) {

    data class Scheduler(
        val enabled: Boolean,
        val delay: Duration,
        val batchSize: Int,
        val maxCatchUpPerTemplate: Int,
        val retryInitialDelay: Duration,
        val retryMaxDelay: Duration,
    ) {

        init {
            require(!delay.isNegative && !delay.isZero) {
                "Recurring transaction scheduler delay must be positive"
            }
            require(batchSize > 0) {
                "Recurring transaction scheduler batch size must be positive"
            }
            require(maxCatchUpPerTemplate > 0) {
                "Recurring transaction scheduler catch-up limit must be positive"
            }
            require(!retryInitialDelay.isNegative && !retryInitialDelay.isZero) {
                "Recurring transaction scheduler retry initial delay must be positive"
            }
            require(retryMaxDelay >= retryInitialDelay) {
                "Recurring transaction scheduler retry max delay must not be less than the initial delay"
            }
        }
    }
}
