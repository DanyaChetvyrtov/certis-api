package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import ru.digitalhustle.certis.enums.TransactionType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class CreateTransactionRq(

    val accountId: UUID,

    val type: TransactionType,

    @field:Positive
    @field:Digits(integer = 15, fraction = 4)
    val amount: BigDecimal,

    val categoryId: UUID?,

    @field:Size(max = 255)
    @field:Pattern(regexp = "(?s).*\\S.*")
    val merchant: String?,

    val note: String?,

    val date: OffsetDateTime,
)
