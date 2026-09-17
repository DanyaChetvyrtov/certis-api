package ru.digitalhustle.certis.features.budget.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.budget.model.BudgetForecastItem
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Component
class BudgetForecastFingerprintCalculator {

    fun sourceFingerprint(items: Collection<BudgetForecastItem>): String =
        fingerprint(
            items
                .filterNot { it.sourceType.name == MANUAL_SOURCE_TYPE }
                .sortedBy(BudgetForecastItem::sourceKey)
                .joinToString(LINE_SEPARATOR, transform = ::sourceLine),
        )

    fun forecastFingerprint(
        sourceFingerprint: String,
        items: Collection<BudgetForecastItem>,
    ): String =
        fingerprint(
            buildString {
                append(sourceFingerprint)
                append(LINE_SEPARATOR)
                append(
                    items.sortedBy(BudgetForecastItem::sourceKey)
                        .joinToString(LINE_SEPARATOR, transform = ::inputLine),
                )
            },
        )

    private fun sourceLine(item: BudgetForecastItem): String =
        listOf(
            item.sourceKey,
            item.sourceType.name,
            item.operationType.name,
            item.category?.id,
            item.expectedDate,
            normalize(item.originalAmount),
            item.defaultConstraintRole,
            item.recurring?.templateId,
            item.recurring?.frequency,
            item.transactionId,
            item.sourceUpdatedAt,
            item.history?.monthsUsed,
            item.history?.method,
            item.sourcePayload.toSortedMap().entries.joinToString(",") { (key, value) -> "$key=$value" },
        ).joinToString(FIELD_SEPARATOR)

    private fun inputLine(item: BudgetForecastItem): String =
        listOf(
            sourceLine(item),
            item.included,
            normalize(item.effectiveAmount),
            item.manualClientId,
            item.title,
        ).joinToString(FIELD_SEPARATOR)

    private fun fingerprint(value: String): String {
        val digest = MessageDigest.getInstance(ALGORITHM)
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(EMPTY) { byte -> BYTE_FORMAT.format(byte) }
        return "$PREFIX$digest"
    }

    private fun normalize(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

    companion object {
        private const val ALGORITHM = "SHA-256"
        private const val PREFIX = "sha256:"
        private const val MANUAL_SOURCE_TYPE = "MANUAL"
        private const val FIELD_SEPARATOR = "|"
        private const val LINE_SEPARATOR = "\n"
        private const val EMPTY = ""
        private const val BYTE_FORMAT = "%02x"
    }
}
