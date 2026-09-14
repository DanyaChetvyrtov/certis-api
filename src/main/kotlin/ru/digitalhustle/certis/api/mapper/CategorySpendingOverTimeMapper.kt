package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.api.dto.request.SpendingOverTimeRq
import ru.digitalhustle.certis.api.dto.response.CategorySeries
import ru.digitalhustle.certis.api.dto.response.CategorySpendingSeries
import ru.digitalhustle.certis.api.dto.response.SpendingOverTimeRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingOverTime
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingOverTimeFilter
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingPoint
import ru.digitalhustle.certis.features.category.query.model.CategorySpendingSeriesData

@Mapper(config = BaseMapperConfig::class)
interface CategorySpendingOverTimeMapper {

    fun convert(source: SpendingOverTimeRq): CategorySpendingOverTimeFilter

    fun convert(source: CategorySpendingOverTime): SpendingOverTimeRs

    fun convert(source: CategorySpendingSeriesData): CategorySpendingSeries

    fun convert(source: CategorySpendingPoint): CategorySeries
}
