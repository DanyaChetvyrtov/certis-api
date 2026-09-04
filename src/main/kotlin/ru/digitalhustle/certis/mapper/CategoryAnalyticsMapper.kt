package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.request.CategoryAnalyticsRq
import ru.digitalhustle.certis.dto.response.CategoryAnalyticsRs
import ru.digitalhustle.certis.dto.response.TopCategoryAnalyticsRs
import ru.digitalhustle.certis.model.category.CategoryAnalytics
import ru.digitalhustle.certis.model.category.CategoryAnalyticsFilter
import ru.digitalhustle.certis.model.category.TopCategoryAnalytics

@Mapper(config = BaseMapperConfig::class)
interface CategoryAnalyticsMapper {

    fun convert(source: CategoryAnalyticsRq): CategoryAnalyticsFilter

    @Mapping(target = "month", expression = "java(source.getMonth().toString())")
    fun convert(source: CategoryAnalytics): CategoryAnalyticsRs

    fun convert(source: TopCategoryAnalytics): TopCategoryAnalyticsRs
}
