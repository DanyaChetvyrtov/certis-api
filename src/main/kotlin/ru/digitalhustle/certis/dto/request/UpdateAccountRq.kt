package ru.digitalhustle.certis.dto.request

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import ru.digitalhustle.certis.enums.AccountType
import java.math.BigDecimal

data class UpdateAccountRq(

    @field:NotBlank
    @field:Size(max = 100, message = "should be less than {max}")
    val name: String,

    val type: AccountType,

    @field:Digits(integer = 15, fraction = 4)
    val openingBalance: BigDecimal,
)
