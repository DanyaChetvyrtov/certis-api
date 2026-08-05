package ru.digitalhustle.certis.dto

import com.fasterxml.jackson.annotation.JsonInclude
import ru.digitalhustle.certis.enums.AccountType
import ru.digitalhustle.certis.enums.Currency
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AccountDto(

    val id: UUID,

    val name: String,

    val type: AccountType,

    val openingBalance: BigDecimal,

    val balance: BigDecimal,

    val currency: Currency,

    val createdAt: OffsetDateTime,

    val closedAt: OffsetDateTime?,
)
