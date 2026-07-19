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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.PasswordsDoNotMatchException
import ru.digitalhustle.certis.model.UserCredentials
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.model.security.JwtData
import ru.digitalhustle.certis.service.domain.UserService
import ru.digitalhustle.certis.service.security.JwtTokenProvider
import ru.digitalhustle.certis.service.security.impl.AuthServiceImpl
import java.time.LocalDateTime
import java.util.UUID

class AuthServiceImplTest {

    private val userService = mock(UserService::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val jwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val authenticationManager = mock(AuthenticationManager::class.java)

    private val authService = AuthServiceImpl(
        userService = userService,
        passwordEncoder = passwordEncoder,
        jwtTokenProvider = jwtTokenProvider,
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
        val userCredentials = UserCredentials(
            email = EMAIL,
            password = PASSWORD,
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

        verify(passwordEncoder)
            .encode(PASSWORD)

        verify(userService)
            .save(EMAIL, ENCODED_PASSWORD)
    }

    @Test
    fun `should throw passwords do not match exception when password confirmation is different`() {
        // given
        val userCredentials = UserCredentials(
            email = EMAIL,
            password = PASSWORD,
            passwordConfirmation = "different_password",
        )

        // when, then
        assertThatThrownBy {
            authService.register(userCredentials)
        }.isInstanceOf(PasswordsDoNotMatchException::class.java)
            .hasMessage(ErrorMessages.PASSWORDS_MISMATCH)

        verifyNoInteractions(passwordEncoder)
        verifyNoInteractions(userService)
    }

    @Test
    fun `should login user`() {
        // given
        val user = createUser()
        val userCredentials = UserCredentials(
            email = EMAIL,
            password = PASSWORD,
            passwordConfirmation = null,
        )

        `when`(userService.getUserByEmail(EMAIL))
            .thenReturn(user)

        `when`(jwtTokenProvider.createAccessToken(user.id, user.email))
            .thenReturn(ACCESS_TOKEN)

        `when`(jwtTokenProvider.createRefreshToken(user.id, user.email))
            .thenReturn(REFRESH_TOKEN)

        // when
        val jwtData = authService.login(userCredentials)

        // then
        assertThat(jwtData)
            .isEqualTo(
                JwtData(
                    id = user.id,
                    email = user.email,
                    accessToken = ACCESS_TOKEN,
                    refreshToken = REFRESH_TOKEN,
                ),
            )

        val authenticationCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken::class.java)
        verify(authenticationManager)
            .authenticate(authenticationCaptor.capture())

        assertAll(
            { assertThat(authenticationCaptor.value.principal).isEqualTo(EMAIL) },
            { assertThat(authenticationCaptor.value.credentials).isEqualTo(PASSWORD) },
        )

        verify(userService)
            .updateLastLogin(user.id)
    }

    @Test
    fun `should refresh access token`() {
        // given
        val jwtData = createJwtData()

        `when`(jwtTokenProvider.refreshUserTokens(REFRESH_TOKEN))
            .thenReturn(jwtData)

        // when
        val refreshedJwtData = authService.refreshAccess(REFRESH_TOKEN)

        // then
        assertThat(refreshedJwtData).isEqualTo(jwtData)

        verify(jwtTokenProvider)
            .refreshUserTokens(REFRESH_TOKEN)
    }

    @Test
    fun `should refresh both tokens`() {
        // given
        val jwtData = createJwtData()

        `when`(jwtTokenProvider.refreshUserTokens(REFRESH_TOKEN))
            .thenReturn(jwtData)

        // when
        val refreshedJwtData = authService.refreshTokens(REFRESH_TOKEN)

        // then
        assertThat(refreshedJwtData).isEqualTo(jwtData)

        verify(jwtTokenProvider)
            .refreshUserTokens(REFRESH_TOKEN)
    }

    @Test
    fun `should not update last login when authentication fails`() {
        // given
        val user = createUser()
        val userCredentials = UserCredentials(
            email = EMAIL,
            password = PASSWORD,
            passwordConfirmation = null,
        )
        val exception = RuntimeException("Authentication failed")

        `when`(userService.getUserByEmail(EMAIL))
            .thenReturn(user)

        doThrow(exception)
            .`when`(authenticationManager)
            .authenticate(any(UsernamePasswordAuthenticationToken::class.java))

        // when, then
        assertThatThrownBy {
            authService.login(userCredentials)
        }.isSameAs(exception)

        verify(userService, never())
            .updateLastLogin(user.id)

        verify(jwtTokenProvider, never())
            .createAccessToken(user.id, user.email)

        verify(jwtTokenProvider, never())
            .createRefreshToken(user.id, user.email)
    }

    private fun createUser(
        id: UUID = UUID.randomUUID(),
        email: String = EMAIL,
        passwordHash: String = PASSWORD_HASH,
    ): User =
        User(
            id = id,
            email = email,
            passwordHash = passwordHash,
            lastLogin = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
        )

    private fun createJwtData(): JwtData =
        JwtData(
            id = UUID.randomUUID(),
            email = EMAIL,
            accessToken = ACCESS_TOKEN,
            refreshToken = REFRESH_TOKEN,
        )
}
