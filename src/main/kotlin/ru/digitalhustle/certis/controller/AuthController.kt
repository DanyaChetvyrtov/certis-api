package ru.digitalhustle.certis.controller

import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.request.LoginRq
import ru.digitalhustle.certis.dto.request.RegisterRq

// TODO Добавить reset пароля
// TODO написать тесты
// TODO протестировать всю логику авторизации
@RequestMapping(PathConstants.AUTH)
interface AuthController {

    @PostMapping
    fun login(@RequestBody loginRequest: @Valid LoginRq, response: HttpServletResponse)

    @PostMapping(PathConstants.REGISTRATION)
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody registerRq: RegisterRq)

    @PostMapping(PathConstants.TOKENS_ACCESS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun refreshAccess(@RequestBody refreshToken: String, response: HttpServletResponse)

    @PostMapping(PathConstants.TOKENS_BOTH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun refreshBothTokens(@RequestBody refreshToken: String, response: HttpServletResponse)
}
