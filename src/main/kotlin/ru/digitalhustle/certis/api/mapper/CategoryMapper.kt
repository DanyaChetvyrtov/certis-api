package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.CategoryDto
import ru.digitalhustle.certis.api.dto.CategoryOptionDto
import ru.digitalhustle.certis.api.dto.request.CategoryCardPageRq
import ru.digitalhustle.certis.api.dto.request.CreateCategoryRq
import ru.digitalhustle.certis.api.dto.request.UpdateCategoryRq
import ru.digitalhustle.certis.api.dto.response.CategoryCardsRs
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.category.command.model.NewCategory
import ru.digitalhustle.certis.features.category.command.model.UpdateCategoryData
import ru.digitalhustle.certis.features.category.model.CategoryPreview
import ru.digitalhustle.certis.features.category.query.model.CategoryCardFilter
import ru.digitalhustle.certis.features.category.query.model.CategoryCards
import ru.digitalhustle.certis.features.category.query.model.CategoryOption
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface CategoryMapper {

    fun convert(source: CreateCategoryRq, userId: UUID): NewCategory

    fun convert(source: UpdateCategoryRq, id: UUID, userId: UUID): UpdateCategoryData

    fun convert(source: CategoryPreview): CategoryDto

    fun convert(source: CategoryCardPageRq): CategoryCardFilter

    @Mapping(target = "month", expression = "java(source.getMonth().toString())")
    fun convert(source: CategoryCards): CategoryCardsRs

    fun convert(source: List<CategoryOption>): List<CategoryOptionDto>
}
