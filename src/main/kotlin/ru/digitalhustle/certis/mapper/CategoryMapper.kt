package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.CategoryDto
import ru.digitalhustle.certis.dto.request.CreateCategoryRq
import ru.digitalhustle.certis.dto.request.UpdateCategoryRq
import ru.digitalhustle.certis.model.CategoryPreview
import ru.digitalhustle.certis.model.NewCategory
import ru.digitalhustle.certis.model.UpdateCategoryData
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface CategoryMapper {

    fun convert(source: CreateCategoryRq, userId: UUID): NewCategory

    fun convert(source: UpdateCategoryRq, id: UUID, userId: UUID): UpdateCategoryData

    fun convert(source: CategoryPreview): CategoryDto
}
