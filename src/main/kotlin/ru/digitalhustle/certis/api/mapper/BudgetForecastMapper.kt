package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.BudgetForecastHistorySourceDto
import ru.digitalhustle.certis.api.dto.BudgetForecastHistoryWindowDto
import ru.digitalhustle.certis.api.dto.BudgetForecastItemDto
import ru.digitalhustle.certis.api.dto.BudgetForecastRecurringSourceDto
import ru.digitalhustle.certis.api.dto.BudgetForecastSummaryDto
import ru.digitalhustle.certis.api.dto.BudgetForecastWarningDto
import ru.digitalhustle.certis.api.dto.CategoryOptionDto
import ru.digitalhustle.certis.api.dto.request.BudgetForecastManualAdjustmentRq
import ru.digitalhustle.certis.api.dto.request.BudgetForecastOverrideRq
import ru.digitalhustle.certis.api.dto.request.ConfirmBudgetForecastRq
import ru.digitalhustle.certis.api.dto.response.BudgetForecastPreviewRs
import ru.digitalhustle.certis.api.dto.response.BudgetForecastRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.budget.command.model.BudgetForecastManualAdjustmentData
import ru.digitalhustle.certis.features.budget.command.model.BudgetForecastOverrideData
import ru.digitalhustle.certis.features.budget.command.model.ConfirmBudgetForecastData
import ru.digitalhustle.certis.features.budget.model.BudgetForecast
import ru.digitalhustle.certis.features.budget.model.BudgetForecastCategory
import ru.digitalhustle.certis.features.budget.model.BudgetForecastHistorySource
import ru.digitalhustle.certis.features.budget.model.BudgetForecastHistoryWindow
import ru.digitalhustle.certis.features.budget.model.BudgetForecastItem
import ru.digitalhustle.certis.features.budget.model.BudgetForecastPreview
import ru.digitalhustle.certis.features.budget.model.BudgetForecastRecurringSource
import ru.digitalhustle.certis.features.budget.model.BudgetForecastSummary
import ru.digitalhustle.certis.features.budget.model.BudgetForecastWarning
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface BudgetForecastMapper {

    fun convert(
        source: ConfirmBudgetForecastRq,
        planId: UUID,
        userId: UUID,
    ): ConfirmBudgetForecastData

    fun convert(source: BudgetForecastOverrideRq): BudgetForecastOverrideData

    fun convert(source: BudgetForecastManualAdjustmentRq): BudgetForecastManualAdjustmentData

    fun convert(source: BudgetForecastPreview): BudgetForecastPreviewRs

    @Mapping(target = "inputFingerprint", source = "forecastFingerprint")
    fun convert(source: BudgetForecast): BudgetForecastRs

    fun convert(source: BudgetForecastItem): BudgetForecastItemDto

    fun convert(source: BudgetForecastCategory): CategoryOptionDto

    fun convert(source: BudgetForecastSummary): BudgetForecastSummaryDto

    @Mapping(target = "fromMonth", expression = "java(source.getFromMonth().toString())")
    @Mapping(target = "toMonth", expression = "java(source.getToMonth().toString())")
    fun convert(source: BudgetForecastHistoryWindow): BudgetForecastHistoryWindowDto

    fun convert(source: BudgetForecastRecurringSource): BudgetForecastRecurringSourceDto

    fun convert(source: BudgetForecastHistorySource): BudgetForecastHistorySourceDto

    fun convert(source: BudgetForecastWarning): BudgetForecastWarningDto
}
