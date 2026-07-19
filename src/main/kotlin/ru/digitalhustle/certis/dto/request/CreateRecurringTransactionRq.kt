package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import ru.digitalhustle.certis.enums.RecurringTransactionFrequency
import ru.digitalhustle.certis.enums.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreateRecurringTransactionRq(

    val accountId: UUID,

    val categoryId: UUID?,

    @field:NotBlank
    @field:Size(max = 150)
    val name: String,

    val type: TransactionType,

    @field:Positive
    @field:Digits(integer = 15, fraction = 4)
    val amount: BigDecimal,

    @field:Size(max = 255)
    @field:Pattern(regexp = "(?s).*\\S.*")
    val merchant: String?,

    val note: String?,

    val frequency: RecurringTransactionFrequency,

    @field:Positive
    val intervalCount: Short,

    val startDate: LocalDate,

    val endDate: LocalDate?,
)
