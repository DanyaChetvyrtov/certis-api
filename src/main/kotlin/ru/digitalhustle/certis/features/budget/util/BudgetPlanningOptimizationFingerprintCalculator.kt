package ru.digitalhustle.certis.features.budget.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.model.BudgetConstraintSet
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Component
class BudgetPlanningOptimizationFingerprintCalculator {

    fun calculate(
        forecast: BudgetForecast,
        constraints: BudgetConstraintSet,
        targetSavingsAmount: BigDecimal,
    ): String {
        val payload = buildString {
            append(ALGORITHM_VERSION)
            append('|')
            append(forecast.id)
            append('|')
            append(forecast.forecastFingerprint)
            append('|')
            append(requireNotNull(constraints.id))
            append('|')
            append(requireNotNull(constraints.constraintFingerprint))
            append('|')
            append(targetSavingsAmount.canonical())
        }
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
            .digest(payload.toByteArray(StandardCharsets.UTF_8))
            .joinToString(EMPTY) { byte -> BYTE_FORMAT.format(byte) }
        return "$PREFIX$digest"
    }

    private fun BigDecimal.canonical(): String = stripTrailingZeros().toPlainString()

    companion object {
        const val ALGORITHM_VERSION = "mckp-v1"
        private const val DIGEST_ALGORITHM = "SHA-256"
        private const val PREFIX = "sha256:"
        private const val EMPTY = ""
        private const val BYTE_FORMAT = "%02x"
    }
}
