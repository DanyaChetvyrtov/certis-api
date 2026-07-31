package ru.digitalhustle.certis.fixture

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.repository.UserRepository
import java.time.OffsetDateTime
import java.util.UUID

@Component
class UserFixture(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun create(
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
