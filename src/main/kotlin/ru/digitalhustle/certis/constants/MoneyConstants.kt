package ru.digitalhustle.certis.constants

import java.math.BigDecimal

object MoneyConstants {

    const val MONEY_SCALE = 2
    const val MONEY_PRECISION = 19

    const val PERCENTAGE_SCALE = 2
    const val PERCENTAGE_PRECISION = 5

    val PERCENTAGE_MULTIPLIER = BigDecimal("100")
}
