package ru.digitalhustle.certis.features.security.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.security.command.repository.UserRepository
import ru.digitalhustle.certis.features.security.command.service.UserService
import ru.digitalhustle.certis.features.security.model.User
import ru.digitalhustle.certis.util.normalizer.EmailNormalizer
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val applicationClock: ApplicationClock,
) : UserService {

    override fun getUserById(id: UUID): User =
        userRepository.findById(id)
            ?: throw NotFoundException.entity("User")

    override fun save(email: String, password: String): User {
        val now = applicationClock.now()
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
        val user = getUserById(id)

        userRepository.save(
            user.copy(
                lastLogin = applicationClock.now(),
            ),
        )
    }

    override fun updatePreferredCurrency(id: UUID, preferredCurrency: Currency) {
        val user = getUserById(id)

        userRepository.save(
            user.copy(
                preferredCurrency = preferredCurrency,
            ),
        )
    }

    override fun delete(id: UUID): Unit =
        userRepository.deleteById(id)

    override fun findPreferredCurrencyForUpdate(userId: UUID): Currency? =
        userRepository.findPreferredCurrencyForUpdate(userId)
}
