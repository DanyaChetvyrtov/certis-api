package ru.digitalhustle.certis.controller.impl

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.AuthController
import ru.digitalhustle.certis.dto.request.LoginRq
import ru.digitalhustle.certis.dto.request.RegisterRq
import ru.digitalhustle.certis.dto.response.SessionsRs
import ru.digitalhustle.certis.mapper.AuthSessionMapper
import ru.digitalhustle.certis.mapper.UserMapper
import ru.digitalhustle.certis.model.security.JwtData
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.security.AuthService
import ru.digitalhustle.certis.service.security.JwtCookieManager
import java.util.UUID

@RestController
class AuthControllerImpl(
    private val userMapper: UserMapper,
    private val authSessionMapper: AuthSessionMapper,
    private val cookieManager: JwtCookieManager,
    private val authService: AuthService,
) : AuthController {

    override fun getSessions(jwtDetails: JwtDetails): SessionsRs {
        val sessions = authService.getSessions(jwtDetails.id).map(authSessionMapper::convert)

        return SessionsRs(sessions)
    }

    override fun login(loginRequest: LoginRq, response: HttpServletResponse) {
        val user = userMapper.convert(loginRequest)
        val jwtData = authService.login(user)

        addCookieToResponse(jwtData, response)
    }

    override fun register(registerRq: RegisterRq) {
        val user = userMapper.convert(registerRq)

        authService.register(user)
    }

    override fun refreshTokens(refreshToken: String?, response: HttpServletResponse) {
        val jwtData = authService.refreshTokens(refreshToken)

        addCookieToResponse(jwtData, response)
    }

    override fun logout(refreshToken: String?, response: HttpServletResponse) {
        authService.logout(refreshToken)
        addRemovalCookiesToResponse(response)
    }

    override fun revokeSession(sessionId: UUID, jwtDetails: JwtDetails) {
        authService.revokeSession(jwtDetails.id, sessionId)
    }

    override fun revokeAllSessions(jwtDetails: JwtDetails, response: HttpServletResponse) {
        authService.revokeAllSessions(jwtDetails.id)
        addRemovalCookiesToResponse(response)
    }

    private fun addCookieToResponse(jwtData: JwtData, response: HttpServletResponse) {
        val refreshCookie = cookieManager.createRefreshTokenCookie(jwtData.refreshToken)

        addAccessCookieToResponse(jwtData, response)
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString())
    }

    private fun addAccessCookieToResponse(jwtData: JwtData, response: HttpServletResponse) {
        val accessCookie = cookieManager.createAccessTokenCookie(jwtData.accessToken)

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString())
    }

    private fun addRemovalCookiesToResponse(response: HttpServletResponse) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieManager.createAccessTokenRemovalCookie().toString())
        response.addHeader(HttpHeaders.SET_COOKIE, cookieManager.createRefreshTokenRemovalCookie().toString())
    }
}
