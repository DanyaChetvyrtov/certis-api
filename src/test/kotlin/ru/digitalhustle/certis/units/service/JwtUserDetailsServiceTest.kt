package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.domain.UserService
import ru.digitalhustle.certis.service.security.impl.JwtUserDetailsService
import java.time.OffsetDateTime
import java.util.UUID

class JwtUserDetailsServiceTest {

    private val userService = mock(UserService::class.java)
    private val jwtUserDetailsService = JwtUserDetailsService(userService)

    private companion object {
        private const val EMAIL = "user@test.com"
        private const val PASSWORD_HASH = "password_hash"
    }

    @Test
    fun `should load user details by email`() {
        // given
        val user = User(
            id = UUID.randomUUID(),
            email = EMAIL,
            passwordHash = PASSWORD_HASH,
            lastLogin = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
        )

        `when`(userService.getUserByEmail(EMAIL))
            .thenReturn(user)

        // when
        val userDetails = jwtUserDetailsService.loadUserByUsername(EMAIL)

        // then
        assertAll(
            { assertThat(userDetails).isInstanceOf(JwtDetails::class.java) },
            { assertThat(userDetails.username).isEqualTo(EMAIL) },
            { assertThat(userDetails.password).isEqualTo(PASSWORD_HASH) },
            { assertThat(userDetails.authorities).isEmpty() },
            { assertThat((userDetails as JwtDetails).id).isEqualTo(user.id) },
        )

        verify(userService)
            .getUserByEmail(EMAIL)
    }
}
