package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.UserDto
import ru.digitalhustle.certis.dto.request.LoginRq
import ru.digitalhustle.certis.dto.request.RegisterRq
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.model.security.UserCredentials

@Mapper(config = BaseMapperConfig::class)
interface UserMapper {

    @Mapping(target = "passwordConfirmation", ignore = true)
    fun convert(source: LoginRq): UserCredentials

    fun convert(source: RegisterRq): UserCredentials

    fun convert(source: User): UserDto
}
