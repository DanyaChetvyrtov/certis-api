package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.InvalidTokenException
import ru.digitalhustle.certis.exception.custom.MissedTokenException
import ru.digitalhustle.certis.exception.custom.PasswordsDoNotMatchException
import ru.digitalhustle.certis.model.entity.RefreshSession
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.model.security.JwtData
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.model.security.RefreshTokenPayload
import ru.digitalhustle.certis.model.security.UserCredentials
import ru.digitalhustle.certis.service.domain.RefreshSessionService
import ru.digitalhustle.certis.service.domain.UserService
import ru.digitalhustle.certis.service.security.JwtTokenProvider
import ru.digitalhustle.certis.service.security.impl.AuthServiceImpl
import java.time.OffsetDateTime
import java.util.UUID

class AuthServiceImplTest {

    private val userService = mock(UserService::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val jwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val refreshSessionService = mock(RefreshSessionService::class.java)
    private val authenticationManager = mock(AuthenticationManager::class.java)

    private val authService = AuthServiceImpl(
        userService = userService,
        passwordEncoder = passwordEncoder,
        jwtTokenProvider = jwtTokenProvider,
        refreshSessionService = refreshSessionService,
        authenticationManager = authenticationManager,
    )

    private companion object {
        private const val EMAIL = "user@test.com"
        private const val PASSWORD = "password"
        private const val PASSWORD_HASH = "password_hash"
        private const val ENCODED_PASSWORD = "encoded_password"
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }

    @Test
    fun `should register user`() {
        // given
        val user = createUser(passwordHash = ENCODED_PASSWORD)
        val userCredentials = createCredentials(
            email = "  USER@TEST.COM ",
            passwordConfirmation = PASSWORD,
        )

        `when`(passwordEncoder.encode(PASSWORD))
            .thenReturn(ENCODED_PASSWORD)
        `when`(userService.save(EMAIL, ENCODED_PASSWORD))
            .thenReturn(user)

        // when
        val registeredUser = authService.register(userCredentials)

        // then
        assertThat(registeredUser).isEqualTo(user)
        verify(passwordEncoder).encode(PASSWORD)
        verify(userService).save(EMAIL, ENCODED_PASSWORD)
    }

    @Test
    fun `should throw passwords do not match exception when password confirmation is different`() {
        // given
        val userCredentials = createCredentials(passwordConfirmation = "different_password")

        // when, then
        assertThatThrownBy {
            authService.register(userCredentials)
        }.isInstanceOf(PasswordsDoNotMatchException::class.java)
            .hasMessage(ErrorMessages.PASSWORDS_MISMATCH)

        verifyNoInteractions(passwordEncoder)
        verifyNoInteractions(userService)
    }

    @Test
    fun `should login user and create refresh session`() {
        // given
        val user = createUser()
        val session = createRefreshSession(user.id)
        val principal = JwtDetails(user.id, user.email, user.passwordHash)
        val authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())

        `when`(
            authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken::class.java)),
        ).thenReturn(authentication)
        `when`(refreshSessionService.create(user.id)).thenReturn(session)
        `when`(jwtTokenProvider.createAccessToken(user.id, user.email)).thenReturn(ACCESS_TOKEN)
        `when`(
            jwtTokenProvider.createRefreshToken(user.id, user.email, session.id, session.expiresAt.toInstant()),
        ).thenReturn(REFRESH_TOKEN)

        // when
        val jwtData = authService.login(createCredentials(email = "  USER@TEST.COM "))

        // then
        assertThat(jwtData).isEqualTo(createJwtData(user.id))

        val authenticationCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken::class.java)
        verify(authenticationManager).authenticate(authenticationCaptor.capture())

        assertAll(
            { assertThat(authenticationCaptor.value.principal).isEqualTo(EMAIL) },
            { assertThat(authenticationCaptor.value.credentials).isEqualTo(PASSWORD) },
        )

        verify(userService).updateLastLogin(user.id)
        verify(userService, never()).getUserByEmail(EMAIL)
        verify(refreshSessionService).create(user.id)
    }

    @Test
    fun `should rotate refresh session and issue a new token pair`() {
        // given
        val userId = UUID.randomUUID()
        val oldSessionId = UUID.randomUUID()
        val newSession = createRefreshSession(userId)
        val payload = RefreshTokenPayload(oldSessionId, userId, EMAIL)

        `when`(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).thenReturn(payload)
        `when`(refreshSessionService.rotate(oldSessionId, userId)).thenReturn(newSession)
        `when`(jwtTokenProvider.createAccessToken(userId, EMAIL)).thenReturn(ACCESS_TOKEN)
        `when`(
            jwtTokenProvider.createRefreshToken(
                userId,
                EMAIL,
                newSession.id,
                newSession.expiresAt.toInstant(),
            ),
        ).thenReturn(REFRESH_TOKEN)

        // when
        val refreshedJwtData = authService.refreshTokens(REFRESH_TOKEN)

        // then
        assertThat(refreshedJwtData).isEqualTo(createJwtData(userId))
        verify(jwtTokenProvider).parseRefreshToken(REFRESH_TOKEN)
        verify(refreshSessionService).rotate(oldSessionId, userId)
    }

    @Test
    fun `should reject refresh when cookie is missing`() {
        assertThatThrownBy {
            authService.refreshTokens(null)
        }.isInstanceOf(MissedTokenException::class.java)
            .hasMessage(ErrorMessages.INVALID_TOKEN)

        verifyNoInteractions(jwtTokenProvider)
        verifyNoInteractions(refreshSessionService)
    }

    @Test
    fun `should revoke current session on logout`() {
        // given
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val payload = RefreshTokenPayload(sessionId, userId, EMAIL)
        `when`(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN)).thenReturn(payload)

        // when
        authService.logout(REFRESH_TOKEN)

        // then
        verify(refreshSessionService).revokeBySessionId(sessionId, userId)
    }

    @Test
    fun `should keep logout idempotent when refresh token is invalid`() {
        // given
        `when`(jwtTokenProvider.parseRefreshToken(REFRESH_TOKEN))
            .thenThrow(InvalidTokenException(ErrorMessages.INVALID_TOKEN))

        // when
        authService.logout(REFRESH_TOKEN)

        // then
        verifyNoInteractions(refreshSessionService)
    }

    @Test
    fun `should not update last login or create session when authentication fails`() {
        // given
        val exception = BadCredentialsException(ErrorMessages.INVALID_CREDENTIALS)

        doThrow(exception)
            .`when`(authenticationManager)
            .authenticate(any(UsernamePasswordAuthenticationToken::class.java))

        // when, then
        assertThatThrownBy {
            authService.login(createCredentials())
        }.isSameAs(exception)

        verifyNoInteractions(userService)
        verifyNoInteractions(refreshSessionService)
        verifyNoInteractions(jwtTokenProvider)
    }

    private fun createCredentials(
        email: String = EMAIL,
        passwordConfirmation: String? = null,
    ): UserCredentials =
        UserCredentials(
            email = email,
            password = PASSWORD,
            passwordConfirmation = passwordConfirmation,
        )

    private fun createUser(
        id: UUID = UUID.randomUUID(),
        email: String = EMAIL,
        passwordHash: String = PASSWORD_HASH,
    ): User =
        User(
            id = id,
            email = email,
            passwordHash = passwordHash,
            lastLogin = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
        )

    private fun createRefreshSession(userId: UUID): RefreshSession {
        val now = OffsetDateTime.now()

        return RefreshSession(
            id = UUID.randomUUID(),
            familyId = UUID.randomUUID(),
            userId = userId,
            expiresAt = now.plusDays(7),
            usedAt = null,
            revokedAt = null,
            createdAt = now,
        )
    }

    private fun createJwtData(userId: UUID): JwtData =
        JwtData(
            id = userId,
            email = EMAIL,
            accessToken = ACCESS_TOKEN,
            refreshToken = REFRESH_TOKEN,
        )
}
