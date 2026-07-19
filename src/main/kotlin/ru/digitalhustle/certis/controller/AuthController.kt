package ru.digitalhustle.certis.controller

import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.request.LoginRq
import ru.digitalhustle.certis.dto.request.RegisterRq

// TODO при реге пользователя с email, который уже существует, бросать 409
// TODO написать тесты
// TODO Добавить reset пароля
@RequestMapping(PathConstants.AUTH)
interface AuthController {

    @PostMapping
    fun login(@RequestBody loginRequest: @Valid LoginRq, response: HttpServletResponse)

    @PostMapping(PathConstants.REGISTRATION)
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody registerRq: @Valid RegisterRq)

    @PostMapping(PathConstants.TOKENS_ACCESS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun refreshAccess(@CookieValue("refresh_token") refreshToken: String, response: HttpServletResponse)

    @PostMapping(PathConstants.TOKENS_BOTH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun refreshBothTokens(@CookieValue("refresh_token") refreshToken: String, response: HttpServletResponse)
}
