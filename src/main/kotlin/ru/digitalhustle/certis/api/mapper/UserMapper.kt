package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.UserDto
import ru.digitalhustle.certis.api.dto.request.LoginRq
import ru.digitalhustle.certis.api.dto.request.RegisterRq
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.security.command.model.UserCredentials
import ru.digitalhustle.certis.features.security.model.User

@Mapper(config = BaseMapperConfig::class)
interface UserMapper {

    @Mapping(target = "passwordConfirmation", ignore = true)
    fun convert(source: LoginRq): UserCredentials

    fun convert(source: RegisterRq): UserCredentials

    fun convert(source: User): UserDto
}
