package ru.digitalhustle.certis.features.account.model

import java.math.BigDecimal

fun Account.toPreview(balance: BigDecimal): AccountPreview =
    AccountPreview(
        id = id,
        name = name,
        type = type,
        openingBalance = openingBalance,
        balance = balance,
        currency = currency,
        createdAt = createdAt,
        closedAt = closedAt,
    )
