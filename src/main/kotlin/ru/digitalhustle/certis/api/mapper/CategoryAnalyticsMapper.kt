package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.request.CategoryAnalyticsRq
import ru.digitalhustle.certis.api.dto.response.CategoryAnalyticsRs
import ru.digitalhustle.certis.api.dto.response.TopCategoryAnalyticsRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.category.query.model.CategoryAnalytics
import ru.digitalhustle.certis.features.category.query.model.CategoryAnalyticsFilter
import ru.digitalhustle.certis.features.category.query.model.TopCategoryAnalytics

@Mapper(config = BaseMapperConfig::class)
interface CategoryAnalyticsMapper {

    fun convert(source: CategoryAnalyticsRq): CategoryAnalyticsFilter

    @Mapping(target = "month", expression = "java(source.getMonth().toString())")
    fun convert(source: CategoryAnalytics): CategoryAnalyticsRs

    fun convert(source: TopCategoryAnalytics): TopCategoryAnalyticsRs
}
