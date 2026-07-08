package ru.digitalhustle.certis.service.security.impl

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.config.properties.JwtProperties
import ru.digitalhustle.certis.service.security.JwtCookieManager
import java.time.Duration

@Service
class JwtCookieManagerImpl(
    private val jwtProperties: JwtProperties
) : JwtCookieManager {

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
        private const val STRICT_SAME_SITE = "Strict"
        private const val ALL_PATHS = "/"
    }

    override fun createAccessTokenCookie(accessToken: String): ResponseCookie =
        ResponseCookie.from(ACCESS_TOKEN, accessToken)
            .httpOnly(true)
            .path(ALL_PATHS)
            .sameSite(STRICT_SAME_SITE)
            .maxAge(Duration.ofMinutes(jwtProperties.accessDuration))
            .build()

    override fun createRefreshTokenCookie(refreshToken: String): ResponseCookie =
        ResponseCookie.from(REFRESH_TOKEN, refreshToken)
            .httpOnly(true)
            .path(ALL_PATHS)
            .sameSite(STRICT_SAME_SITE)
            .maxAge(Duration.ofDays(jwtProperties.refreshDuration))
            .build()

    override fun getAccessTokenFromRequest(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == ACCESS_TOKEN }
            ?.value
}