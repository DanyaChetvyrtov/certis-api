package ru.digitalhustle.certis.dto.request

import org.springframework.format.annotation.DateTimeFormat
import ru.digitalhustle.certis.enums.CashFlowRange
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.util.validation.ValidZoneId
import java.time.LocalDate

data class CashFlowAnalyticsRq(

    val range: CashFlowRange,

    val currency: Currency,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    val anchorDate: LocalDate,

    @field:ValidZoneId
    val timeZone: String,
)
