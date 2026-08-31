package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.repository.UserRepository
import ru.digitalhustle.certis.service.domain.impl.UserServiceImpl
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class UserServiceImplTest {

    private val repository = mock(UserRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC)
    private val service = UserServiceImpl(repository, clock)

    @Test
    fun `should read user by id`() {
        // given
        val user = createUser()
        `when`(repository.findById(user.id)).thenReturn(user)

        // when
        val result = service.getUserById(user.id)

        // then
        assertThat(result).isEqualTo(user)
        verify(repository).findById(user.id)
    }

    @Test
    fun `should normalize email when reading user`() {
        // given
        val user = createUser()
        `when`(repository.findByEmail(EMAIL)).thenReturn(user)

        // when
        val result = service.getUserByEmail("  USER@TEST.COM ")

        // then
        assertThat(result).isEqualTo(user)
        verify(repository).findByEmail(EMAIL)
    }

    @Test
    fun `should normalize email before atomic user creation`() {
        // given
        `when`(repository.create(anyUser()))
            .thenAnswer { it.arguments[0] as User }

        // when
        val result = service.save("  USER@TEST.COM ", PASSWORD_HASH)

        // then
        assertThat(result.email).isEqualTo(EMAIL)
        assertThat(result.preferredCurrency).isEqualTo(Currency.USD)
        assertThat(result.lastLogin).isEqualTo(OffsetDateTime.now(clock))
        assertThat(result.createdAt).isEqualTo(OffsetDateTime.now(clock))
        verify(repository).create(anyUser())
    }

    @Test
    fun `should report conflict when concurrent insert loses`() {
        // given
        `when`(repository.create(anyUser())).thenReturn(null)

        // when, then
        assertThatThrownBy {
            service.save(EMAIL, PASSWORD_HASH)
        }.isInstanceOf(EntityAlreadyExistsException::class.java)
            .hasMessage("User with such email already exists")
    }

    @Test
    fun `should update last login with configured clock`() {
        // given
        val user = createUser()
        `when`(repository.findById(user.id)).thenReturn(user)

        // when
        service.updateLastLogin(user.id)

        // then
        verify(repository).save(user.copy(lastLogin = OffsetDateTime.now(clock)))
    }

    @Test
    fun `should update preferred currency`() {
        // given
        val user = createUser()
        `when`(repository.findById(user.id)).thenReturn(user)

        // when
        service.updatePreferredCurrency(user.id, Currency.EUR)

        // then
        verify(repository).save(user.copy(preferredCurrency = Currency.EUR))
    }

    private fun anyUser(): User {
        any(User::class.java)
        return createUser()
    }

    private fun createUser(): User =
        User(
            id = UUID.randomUUID(),
            email = EMAIL,
            passwordHash = PASSWORD_HASH,
            lastLogin = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
        )

    private companion object {
        private const val EMAIL = "user@test.com"
        private const val PASSWORD_HASH = "password_hash"
    }
}
