package ru.digitalhustle.certis.features.account.api

import ru.digitalhustle.certis.enums.Currency
import java.time.OffsetDateTime
import java.util.UUID

data class AccountSnapshot(

    val id: UUID,

    val currency: Currency,

    val closedAt: OffsetDateTime?,
)
