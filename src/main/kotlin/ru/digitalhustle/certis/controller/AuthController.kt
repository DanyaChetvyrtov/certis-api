package ru.digitalhustle.certis.controller

import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.dto.request.LoginRq
import ru.digitalhustle.certis.dto.request.RegisterRq
import ru.digitalhustle.certis.dto.response.SessionsRs
import ru.digitalhustle.certis.model.security.JwtDetails
import java.util.UUID

@RequestMapping(PathConstants.AUTH)
interface AuthController {

    @GetMapping(PathConstants.SESSIONS)
    fun getSessions(@AuthenticationPrincipal jwtDetails: JwtDetails): SessionsRs

    @PostMapping
    fun login(@RequestBody loginRequest: @Valid LoginRq, response: HttpServletResponse)

    @PostMapping(PathConstants.REGISTRATION)
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody registerRq: @Valid RegisterRq)

    @PostMapping(PathConstants.TOKENS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun refreshTokens(
        @CookieValue(name = SecurityConstants.REFRESH_TOKEN_COOKIE, required = false) refreshToken: String?,
        response: HttpServletResponse,
    )

    @PostMapping(PathConstants.LOGOUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @CookieValue(name = SecurityConstants.REFRESH_TOKEN_COOKIE, required = false) refreshToken: String?,
        response: HttpServletResponse,
    )

    @DeleteMapping(PathConstants.SESSIONS_WITH_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeSession(
        @PathVariable sessionId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    )

    @DeleteMapping(PathConstants.SESSIONS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeAllSessions(
        @AuthenticationPrincipal jwtDetails: JwtDetails,
        response: HttpServletResponse,
    )
}
