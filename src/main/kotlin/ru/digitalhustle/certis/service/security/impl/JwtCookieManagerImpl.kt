package ru.digitalhustle.certis.service.security.impl

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.config.properties.JwtProperties
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.service.security.JwtCookieManager
import java.time.Duration

@Service
class JwtCookieManagerImpl(
    private val jwtProperties: JwtProperties,
) : JwtCookieManager {

    companion object {
        private const val STRICT_SAME_SITE = "Strict"
        private const val ALL_PATHS = "/"
    }

    override fun createAccessTokenCookie(accessToken: String): ResponseCookie =
        createCookie(
            name = SecurityConstants.ACCESS_TOKEN_COOKIE,
            value = accessToken,
            path = ALL_PATHS,
            maxAge = jwtProperties.accessDuration,
        )

    override fun createRefreshTokenCookie(refreshToken: String): ResponseCookie =
        createCookie(
            name = SecurityConstants.REFRESH_TOKEN_COOKIE,
            value = refreshToken,
            path = PathConstants.AUTH,
            maxAge = jwtProperties.refreshDuration,
        )

    override fun createAccessTokenRemovalCookie(): ResponseCookie =
        createCookie(
            name = SecurityConstants.ACCESS_TOKEN_COOKIE,
            value = "",
            path = ALL_PATHS,
            maxAge = Duration.ZERO,
        )

    override fun createRefreshTokenRemovalCookie(): ResponseCookie =
        createCookie(
            name = SecurityConstants.REFRESH_TOKEN_COOKIE,
            value = "",
            path = PathConstants.AUTH,
            maxAge = Duration.ZERO,
        )

    override fun getAccessTokenFromRequest(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == SecurityConstants.ACCESS_TOKEN_COOKIE }
            ?.value

    private fun createCookie(
        name: String,
        value: String,
        path: String,
        maxAge: Duration,
    ): ResponseCookie =
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .path(path)
            .sameSite(STRICT_SAME_SITE)
            .maxAge(maxAge)
            .build()
}
