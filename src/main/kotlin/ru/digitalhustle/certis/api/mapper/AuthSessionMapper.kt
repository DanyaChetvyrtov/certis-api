package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.AuthSessionDto
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.security.model.RefreshSession

@Mapper(config = BaseMapperConfig::class)
interface AuthSessionMapper {

    @Mapping(target = "id", source = "familyId")
    @Mapping(target = "lastRefreshedAt", source = "createdAt")
    fun convert(source: RefreshSession): AuthSessionDto
}
