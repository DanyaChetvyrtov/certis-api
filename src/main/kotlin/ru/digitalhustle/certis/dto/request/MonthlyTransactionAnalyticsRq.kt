package ru.digitalhustle.certis.dto.request

import org.springframework.format.annotation.DateTimeFormat
import ru.digitalhustle.certis.enums.Currency
import java.time.YearMonth

data class MonthlyTransactionAnalyticsRq(

    @DateTimeFormat(pattern = "yyyy-MM")
    val month: YearMonth,

    val currency: Currency,
)
