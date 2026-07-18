package ru.digitalhustle.certis.units.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import ru.digitalhustle.certis.config.properties.JwtProperties
import ru.digitalhustle.certis.exception.custom.InvalidTokenException
import ru.digitalhustle.certis.service.security.JwtTokenProvider
import ru.digitalhustle.certis.service.security.impl.JwtTokenProviderImpl
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

class JwtTokenProviderImplTest {

    private lateinit var userDetailsService: UserDetailsService
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(JWT_SECRET.toByteArray())
    private val clock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

    private companion object {
        private const val EMAIL = "user@test.com"
        private const val PASSWORD = "password"
        private const val JWT_SECRET = "01234567890123456789012345678901"
        private const val ACCESS_DURATION = 1L
        private const val REFRESH_DURATION = 30L

        private val NOW: Instant = Instant.parse("2026-07-19T12:00:00Z")
    }

    @BeforeEach
    fun setUp() {
        userDetailsService = mock(UserDetailsService::class.java)
        jwtTokenProvider = createTokenProvider(clock)
    }

    @Test
    fun `should create access token with user claims and access expiration`() {
        // given
        val userId = UUID.randomUUID()

        // when
        val token = jwtTokenProvider.createAccessToken(
            userId = userId,
            email = EMAIL,
        )

        // then
        val claims = parseClaims(token)

        assertAll(
            { assertThat(jwtTokenProvider.isValid(token)).isTrue() },
            { assertThat(claims.subject).isEqualTo(EMAIL) },
            { assertThat(claims["id"]).isEqualTo(userId.toString()) },
            { assertThat(claims.expiration).isEqualTo(Date.from(NOW.plus(ACCESS_DURATION, ChronoUnit.HOURS))) },
        )
    }

    @Test
    fun `should create refresh token with user claims and refresh expiration`() {
        // given
        val userId = UUID.randomUUID()

        // when
        val token = jwtTokenProvider.createRefreshToken(
            userId = userId,
            email = EMAIL,
        )

        // then
        val claims = parseClaims(token)

        assertAll(
            { assertThat(jwtTokenProvider.isValid(token)).isTrue() },
            { assertThat(claims.subject).isEqualTo(EMAIL) },
            { assertThat(claims["id"]).isEqualTo(userId.toString()) },
            { assertThat(claims.expiration).isEqualTo(Date.from(NOW.plus(REFRESH_DURATION, ChronoUnit.DAYS))) },
        )
    }

    @Test
    fun `should refresh user tokens`() {
        // given
        val userId = UUID.randomUUID()
        val refreshToken = jwtTokenProvider.createRefreshToken(
            userId = userId,
            email = EMAIL,
        )

        // when
        val jwtData = jwtTokenProvider.refreshUserTokens(refreshToken)

        // then
        assertAll(
            { assertThat(jwtData.id).isEqualTo(userId) },
            { assertThat(jwtData.email).isEqualTo(EMAIL) },
            { assertThat(parseClaims(jwtData.accessToken).subject).isEqualTo(EMAIL) },
            { assertThat(parseClaims(jwtData.refreshToken).subject).isEqualTo(EMAIL) },
            { assertThat(jwtTokenProvider.isValid(jwtData.accessToken)).isTrue() },
            { assertThat(jwtTokenProvider.isValid(jwtData.refreshToken)).isTrue() },
        )
    }

    @Test
    fun `should return false when token is expired`() {
        // given
        val token = jwtTokenProvider.createAccessToken(
            userId = UUID.randomUUID(),
            email = EMAIL,
        )
        val expiredTokenProvider = createTokenProvider(
            Clock.fixed(NOW.plus(ACCESS_DURATION + 1, ChronoUnit.HOURS), ZoneOffset.UTC),
        )

        // when
        val isValid = expiredTokenProvider.isValid(token)

        // then
        assertThat(isValid).isFalse()
    }

    @Test
    fun `should return false when token is malformed`() {
        // when
        val isValid = jwtTokenProvider.isValid("malformed_token")

        // then
        assertThat(isValid).isFalse()
    }

    @Test
    fun `should throw invalid token exception when refresh token is malformed`() {
        assertThatThrownBy {
            jwtTokenProvider.refreshUserTokens("malformed_token")
        }.isInstanceOf(InvalidTokenException::class.java)
    }

    @Test
    fun `should return authentication by token subject`() {
        // given
        val token = jwtTokenProvider.createAccessToken(
            userId = UUID.randomUUID(),
            email = EMAIL,
        )
        val userDetails = User.withUsername(EMAIL)
            .password(PASSWORD)
            .authorities(emptyList())
            .build()

        `when`(userDetailsService.loadUserByUsername(EMAIL))
            .thenReturn(userDetails)

        // when
        val authentication = jwtTokenProvider.getAuthentication(token)

        // then
        assertAll(
            { assertThat(authentication.principal).isEqualTo(userDetails) },
            { assertThat(authentication.credentials).isEqualTo("") },
            { assertThat(authentication.authorities).isEmpty() },
        )

        verify(userDetailsService)
            .loadUserByUsername(EMAIL)
    }

    private fun createTokenProvider(clock: Clock): JwtTokenProvider =
        JwtTokenProviderImpl(
            jwtProperties = JwtProperties(
                secret = JWT_SECRET,
                accessDuration = ACCESS_DURATION,
                refreshDuration = REFRESH_DURATION,
            ),
            userDetailsService = userDetailsService,
            clock = clock,
        ).also {
            it.init()
        }

    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .clock { Date.from(NOW) }
            .build()
            .parseSignedClaims(token)
            .payload
}
