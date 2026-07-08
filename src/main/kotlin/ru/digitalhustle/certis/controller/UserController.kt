package ru.digitalhustle.certis.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.UserDto
import ru.digitalhustle.certis.dto.request.CreateUserRq
import java.util.*

@RequestMapping(PathConstants.USERS)
interface UserController {

    @GetMapping(PathConstants.USERS_ID)
    fun getById(@PathVariable id: UUID): UserDto

    @PostMapping(PathConstants.USERS)
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody createUserRq: CreateUserRq): UserDto

    @DeleteMapping(PathConstants.USERS_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteById(@PathVariable id: UUID)
}