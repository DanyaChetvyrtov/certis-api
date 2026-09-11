package ru.digitalhustle.certis.fixture

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.security.command.repository.UserRepository
import ru.digitalhustle.certis.features.security.model.User
import java.time.OffsetDateTime
import java.util.UUID

@Component
class UserFixture(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun createInDb(
        block: User.() -> User = { this },
    ): User {
        val user = User(
            id = UUID.randomUUID(),
            email = "user@test.com",
            passwordHash = passwordEncoder.encode("password"),
            lastLogin = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
        ).block()

        return userRepository.save(user)
    }
}
