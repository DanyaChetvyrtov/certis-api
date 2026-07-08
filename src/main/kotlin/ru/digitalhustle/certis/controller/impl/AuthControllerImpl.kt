package ru.digitalhustle.certis.controller.impl

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.AuthController
import ru.digitalhustle.certis.dto.request.LoginRq
import ru.digitalhustle.certis.dto.request.RegisterRq
import ru.digitalhustle.certis.mapper.UserMapper
import ru.digitalhustle.certis.model.security.JwtData
import ru.digitalhustle.certis.service.security.AuthService
import ru.digitalhustle.certis.service.security.JwtCookieManager

@RestController
class AuthControllerImpl(
    private val userMapper: UserMapper,
    private val cookieManager: JwtCookieManager,
    private val authService: AuthService
) : AuthController {

    override fun login(loginRequest: LoginRq, response: HttpServletResponse) {
        val user = userMapper.convert(loginRequest)
        val jwtData = authService.login(user)

        addCookieToResponse(jwtData, response)
    }

    override fun register(registerRq: RegisterRq) {
        val user = userMapper.convert(registerRq)

        authService.register(user)
    }

    override fun refreshAccess(refreshToken: String, response: HttpServletResponse) {
        val jwtData = authService.refreshAccess(refreshToken)

        addCookieToResponse(jwtData, response)
    }

    override fun refreshBothTokens(refreshToken: String, response: HttpServletResponse) {
        val jwtData = authService.refreshTokens(refreshToken)

        addCookieToResponse(jwtData, response)
    }

    private fun addCookieToResponse(jwtData: JwtData, response: HttpServletResponse) {
        val accessCookie = cookieManager.createAccessTokenCookie(jwtData.accessToken)
        val refreshCookie = cookieManager.createRefreshTokenCookie(jwtData.refreshToken)

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString())
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString())
    }
}