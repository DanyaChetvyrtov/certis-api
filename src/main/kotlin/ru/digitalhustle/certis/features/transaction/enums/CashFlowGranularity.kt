package ru.digitalhustle.certis.features.transaction.enums

enum class CashFlowGranularity(
    val databaseValue: String,
) {
    HOUR("hour"),
    DAY("day"),
    MONTH("month"),
}
