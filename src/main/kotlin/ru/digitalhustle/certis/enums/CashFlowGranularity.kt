package ru.digitalhustle.certis.enums

enum class CashFlowGranularity(
    val databaseValue: String,
) {
    HOUR("hour"),
    DAY("day"),
    MONTH("month"),
}
