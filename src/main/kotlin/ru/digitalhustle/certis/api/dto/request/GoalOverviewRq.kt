package ru.digitalhustle.certis.api.dto.request

import org.springframework.format.annotation.DateTimeFormat
import ru.digitalhustle.certis.enums.Currency
import java.time.YearMonth

data class GoalOverviewRq(

    @field:DateTimeFormat(pattern = "yyyy-MM")
    val month: YearMonth? = null,

    val currency: Currency? = null,
)
