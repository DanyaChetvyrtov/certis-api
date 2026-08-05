package ru.digitalhustle.certis.mapper

import ru.digitalhustle.certis.model.AccountPreview
import ru.digitalhustle.certis.model.entity.Account
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
