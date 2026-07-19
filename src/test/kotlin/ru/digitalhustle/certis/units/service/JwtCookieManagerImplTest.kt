package ru.digitalhustle.certis.units.service

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.mock.web.MockHttpServletRequest
import ru.digitalhustle.certis.config.properties.JwtProperties
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.service.security.impl.JwtCookieManagerImpl
import java.time.Duration

class JwtCookieManagerImplTest {

    private val jwtCookieManager = JwtCookieManagerImpl(
        JwtProperties(
            secret = JWT_SECRET,
            accessDuration = ACCESS_DURATION,
            refreshDuration = REFRESH_DURATION,
        ),
    )

    private companion object {
        private const val ACCESS_TOKEN = "access_token_value"
        private const val REFRESH_TOKEN = "refresh_token_value"
        private const val JWT_SECRET = "01234567890123456789012345678901"
        private const val ACCESS_DURATION = 1L
        private const val REFRESH_DURATION = 30L
    }

    @Test
    fun `should create access token cookie`() {
        // when
        val cookie = jwtCookieManager.createAccessTokenCookie(ACCESS_TOKEN)

        // then
        assertAll(
            { assertThat(cookie.name).isEqualTo("access_token") },
            { assertThat(cookie.value).isEqualTo(ACCESS_TOKEN) },
            { assertThat(cookie.isHttpOnly).isTrue() },
            { assertThat(cookie.path).isEqualTo("/") },
            { assertThat(cookie.sameSite).isEqualTo("Strict") },
            { assertThat(cookie.maxAge).isEqualTo(Duration.ofHours(ACCESS_DURATION)) },
        )
    }

    @Test
    fun `should create refresh token cookie`() {
        // when
        val cookie = jwtCookieManager.createRefreshTokenCookie(REFRESH_TOKEN)

        // then
        assertAll(
            { assertThat(cookie.name).isEqualTo("refresh_token") },
            { assertThat(cookie.value).isEqualTo(REFRESH_TOKEN) },
            { assertThat(cookie.isHttpOnly).isTrue() },
            { assertThat(cookie.path).isEqualTo(PathConstants.AUTH_TOKEN) },
            { assertThat(cookie.sameSite).isEqualTo("Strict") },
            { assertThat(cookie.maxAge).isEqualTo(Duration.ofDays(REFRESH_DURATION)) },
        )
    }

    @Test
    fun `should get access token from request cookies`() {
        // given
        val request = MockHttpServletRequest().apply {
            setCookies(
                Cookie("refresh_token", REFRESH_TOKEN),
                Cookie("access_token", ACCESS_TOKEN),
            )
        }

        // when
        val accessToken = jwtCookieManager.getAccessTokenFromRequest(request)

        // then
        assertThat(accessToken).isEqualTo(ACCESS_TOKEN)
    }

    @Test
    fun `should return null when access token cookie is missing`() {
        // given
        val request = MockHttpServletRequest().apply {
            setCookies(Cookie("refresh_token", REFRESH_TOKEN))
        }

        // when
        val accessToken = jwtCookieManager.getAccessTokenFromRequest(request)

        // then
        assertThat(accessToken).isNull()
    }

    @Test
    fun `should return null when request has no cookies`() {
        // given
        val request = MockHttpServletRequest()

        // when
        val accessToken = jwtCookieManager.getAccessTokenFromRequest(request)

        // then
        assertThat(accessToken).isNull()
    }
}
