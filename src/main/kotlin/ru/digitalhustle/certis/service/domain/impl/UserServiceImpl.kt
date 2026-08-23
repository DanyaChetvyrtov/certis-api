package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.repository.UserRepository
import ru.digitalhustle.certis.service.domain.UserService
import ru.digitalhustle.certis.util.normalizer.EmailNormalizer
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val clock: Clock,
) : UserService {

    override fun getUserByEmail(email: String): User =
        userRepository.findByEmail(EmailNormalizer.normalize(email))
            ?: throw NotFoundException.entity("User")

    override fun save(email: String, password: String): User {
        val now = OffsetDateTime.now(clock)
        val normalizedEmail = EmailNormalizer.normalize(email)

        return userRepository.create(
            User(
                id = UUID.randomUUID(),
                email = normalizedEmail,
                passwordHash = password,
                lastLogin = now,
                createdAt = now,
            ),
        ) ?: throw EntityAlreadyExistsException.entity("User", "email")
    }

    override fun updateLastLogin(id: UUID) {
        val user = userRepository.findById(id)
            ?: throw NotFoundException.entity("User")

        userRepository.save(
            user.copy(
                lastLogin = OffsetDateTime.now(clock),
            ),
        )
    }

    override fun delete(id: UUID): Unit =
        userRepository.deleteById(id)
}
