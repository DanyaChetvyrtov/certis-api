package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.UserDto
import ru.digitalhustle.certis.dto.request.LoginRq
import ru.digitalhustle.certis.dto.request.RegisterRq
import ru.digitalhustle.certis.model.UserCredentials
import ru.digitalhustle.certis.model.entity.User

@Mapper(config = BaseMapperConfig::class)
interface UserMapper {

    @Mapping(target = "passwordConfirmation", ignore = true)
    fun convert(loginRq: LoginRq): UserCredentials

    fun convert(registerRq: RegisterRq): UserCredentials

    fun convert(user: User): UserDto
}
