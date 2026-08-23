package ru.digitalhustle.certis.model.account

import java.math.BigDecimal
import java.util.UUID

data class AccountBalanceDelta(
    val accountId: UUID,
    val delta: BigDecimal,
)
