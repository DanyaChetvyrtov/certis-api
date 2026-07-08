package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.UserDto
import ru.digitalhustle.certis.dto.request.CreateUserRq
import ru.digitalhustle.certis.model.entity.User

@Mapper(config = BaseMapperConfig::class)
interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    fun convert(createUserRq: CreateUserRq): User

    fun convert(user: User): UserDto
}