package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.AuthSessionDto
import ru.digitalhustle.certis.model.entity.RefreshSession

@Mapper(config = BaseMapperConfig::class)
interface AuthSessionMapper {

    @Mapping(target = "id", source = "familyId")
    @Mapping(target = "lastRefreshedAt", source = "createdAt")
    fun convert(source: RefreshSession): AuthSessionDto
}
