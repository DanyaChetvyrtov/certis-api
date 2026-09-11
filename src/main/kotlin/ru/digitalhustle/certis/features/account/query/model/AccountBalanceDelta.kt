package ru.digitalhustle.certis.features.account.query.model

import java.math.BigDecimal
import java.util.UUID

data class AccountBalanceDelta(
    val accountId: UUID,
    val delta: BigDecimal,
)
