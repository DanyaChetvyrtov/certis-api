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
import ru.digitalhustle.certis.enums.JwtTokenType
import ru.digitalhustle.certis.exception.custom.InvalidTokenException
import ru.digitalhustle.certis.service.security.JwtTokenProvider
import ru.digitalhustle.certis.service.security.impl.JwtTokenProviderImpl
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
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

        private val ACCESS_DURATION: Duration = Duration.ofMinutes(30)
        private val REFRESH_DURATION = Duration.ofDays(30)
        private val NOW: Instant = Instant.parse("2026-07-19T12:00:00Z")
    }

    @BeforeEach
    fun setUp() {
        userDetailsService = mock(UserDetailsService::class.java)
        jwtTokenProvider = createTokenProvider(clock)
    }

    @Test
    fun `should create access token with access type and expiration`() {
        // given
        val userId = UUID.randomUUID()

        // when
        val token = jwtTokenProvider.createAccessToken(userId, EMAIL)

        // then
        val claims = parseClaims(token)

        assertAll(
            { assertThat(jwtTokenProvider.isValidAccessToken(token)).isTrue() },
            { assertThat(claims.subject).isEqualTo(EMAIL) },
            { assertThat(claims["id"]).isEqualTo(userId.toString()) },
            { assertThat(claims["type"]).isEqualTo(JwtTokenType.ACCESS.name) },
            { assertThat(claims.issuedAt).isEqualTo(Date.from(NOW)) },
            { assertThat(claims.expiration).isEqualTo(Date.from(NOW.plus(ACCESS_DURATION))) },
        )
    }

    @Test
    fun `should create refresh token with refresh type and session id`() {
        // given
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val expiresAt = NOW.plus(REFRESH_DURATION)

        // when
        val token = jwtTokenProvider.createRefreshToken(userId, EMAIL, sessionId, expiresAt)

        // then
        val claims = parseClaims(token)

        assertAll(
            { assertThat(jwtTokenProvider.isValidAccessToken(token)).isFalse() },
            { assertThat(claims.subject).isEqualTo(EMAIL) },
            { assertThat(claims.id).isEqualTo(sessionId.toString()) },
            { assertThat(claims["id"]).isEqualTo(userId.toString()) },
            { assertThat(claims["type"]).isEqualTo(JwtTokenType.REFRESH.name) },
            { assertThat(claims.expiration).isEqualTo(Date.from(expiresAt)) },
        )
    }

    @Test
    fun `should parse refresh token payload`() {
        // given
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val token = jwtTokenProvider.createRefreshToken(
            userId = userId,
            email = EMAIL,
            sessionId = sessionId,
            expiresAt = NOW.plus(REFRESH_DURATION),
        )

        // when
        val payload = jwtTokenProvider.parseRefreshToken(token)

        // then
        assertAll(
            { assertThat(payload.sessionId).isEqualTo(sessionId) },
            { assertThat(payload.userId).isEqualTo(userId) },
            { assertThat(payload.email).isEqualTo(EMAIL) },
        )
    }

    @Test
    fun `should reject access token as refresh token`() {
        // given
        val accessToken = jwtTokenProvider.createAccessToken(UUID.randomUUID(), EMAIL)

        // when, then
        assertThatThrownBy {
            jwtTokenProvider.parseRefreshToken(accessToken)
        }.isInstanceOf(InvalidTokenException::class.java)
    }

    @Test
    fun `should return false when access token is expired`() {
        // given
        val token = jwtTokenProvider.createAccessToken(UUID.randomUUID(), EMAIL)
        val expiredTokenProvider = createTokenProvider(
            Clock.fixed(NOW.plus(ACCESS_DURATION).plusSeconds(1), ZoneOffset.UTC),
        )

        // when
        val isValid = expiredTokenProvider.isValidAccessToken(token)

        // then
        assertThat(isValid).isFalse()
    }

    @Test
    fun `should return false when access token is malformed`() {
        // when
        val isValid = jwtTokenProvider.isValidAccessToken("malformed_token")

        // then
        assertThat(isValid).isFalse()
    }

    @Test
    fun `should throw invalid token exception when refresh token is malformed`() {
        assertThatThrownBy {
            jwtTokenProvider.parseRefreshToken("malformed_token")
        }.isInstanceOf(InvalidTokenException::class.java)
    }

    @Test
    fun `should return authentication by access token subject`() {
        // given
        val token = jwtTokenProvider.createAccessToken(UUID.randomUUID(), EMAIL)
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
