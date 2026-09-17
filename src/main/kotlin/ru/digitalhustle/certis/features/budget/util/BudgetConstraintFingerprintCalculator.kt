package ru.digitalhustle.certis.features.budget.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.model.BudgetCategoryConstraint
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Component
class BudgetConstraintFingerprintCalculator {

    fun calculate(
        forecastFingerprint: String,
        savingsFloorAmount: BigDecimal,
        categories: List<BudgetCategoryConstraint>,
    ): String {
        val payload = buildString {
            append(forecastFingerprint)
            append('|')
            append(savingsFloorAmount.canonical())
            categories.sortedBy { category -> category.category.id }.forEach { category ->
                append('|')
                append(category.category.id)
                append(':')
                append(category.allocationType)
                append(':')
                append(category.constraintRole)
                append(':')
                append(category.requiredAmount.canonical())
                append(':')
                append(category.priority)
                append(':')
                append(category.currentLimitAmount.canonical())
                append(':')
                append(category.sourceKeys.sorted().joinToString(","))
                category.fundingLevels.sortedBy { level -> level.level.ordinal }.forEach { level ->
                    append(':')
                    append(level.level)
                    append('=')
                    append(level.amount.canonical())
                }
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return "sha256:$digest"
    }

    private fun BigDecimal.canonical(): String = stripTrailingZeros().toPlainString()
}
