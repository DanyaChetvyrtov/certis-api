package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.CategoryDto
import ru.digitalhustle.certis.dto.request.CategoryCardPageRq
import ru.digitalhustle.certis.dto.request.CreateCategoryRq
import ru.digitalhustle.certis.dto.request.UpdateCategoryRq
import ru.digitalhustle.certis.dto.response.CategoryCardsRs
import ru.digitalhustle.certis.dto.response.CategoryOptionRs
import ru.digitalhustle.certis.model.CategoryPreview
import ru.digitalhustle.certis.model.NewCategory
import ru.digitalhustle.certis.model.UpdateCategoryData
import ru.digitalhustle.certis.model.category.CategoryCardFilter
import ru.digitalhustle.certis.model.category.CategoryCards
import ru.digitalhustle.certis.model.category.CategoryOption
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface CategoryMapper {

    fun convert(source: CreateCategoryRq, userId: UUID): NewCategory

    fun convert(source: UpdateCategoryRq, id: UUID, userId: UUID): UpdateCategoryData

    fun convert(source: CategoryPreview): CategoryDto

    fun convert(source: CategoryCardPageRq): CategoryCardFilter

    @Mapping(target = "month", expression = "java(source.getMonth().toString())")
    fun convert(source: CategoryCards): CategoryCardsRs

    fun convert(source: List<CategoryOption>): List<CategoryOptionRs>
}
