package ru.digitalhustle.certis.api.dto.request

import ru.digitalhustle.certis.shared.enums.Currency
import java.time.YearMonth

data class CreateBudgetPlanRq(

    val month: YearMonth,

    val currency: Currency,
)
