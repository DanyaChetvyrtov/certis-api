package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.UserController
import ru.digitalhustle.certis.dto.UserDto
import ru.digitalhustle.certis.dto.request.CreateUserRq
import ru.digitalhustle.certis.mapper.UserMapper
import ru.digitalhustle.certis.service.domain.UserService
import java.util.*

@RestController
class UserControllerImpl(
    private val userService: UserService,
    private val userMapper: UserMapper
) : UserController {

    override fun getById(id: UUID): UserDto {
        val user = userService.getById(id)
        return userMapper.convert(user)
    }

    override fun create(createUserRq: CreateUserRq): UserDto {
        val user = userMapper.convert(createUserRq)
        val dbUser = userService.create(user)
        return userMapper.convert(dbUser)
    }

    override fun deleteById(id: UUID): Unit=
        userService.deleteById(id)
}