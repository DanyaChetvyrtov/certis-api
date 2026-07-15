package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.NotFoundException
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.repository.UserRepository
import ru.digitalhustle.certis.service.domain.UserService
import java.time.LocalDateTime
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
) : UserService {

    override fun getUserByEmail(email: String): User =
        userRepository.findByEmail(email)
            ?: throw NotFoundException.entity("User")

    override fun save(email: String, password: String): User {
        val preparedUser = User(
            id = UUID.randomUUID(),
            email = email,
            password = password,
            lastLogin = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
        )

        return userRepository.save(preparedUser)
    }

    override fun updateLastLogin(id: UUID) {
        val user = userRepository.findById(id)
            ?: throw NotFoundException.entity("User")

        userRepository.save(
            user.copy(
                lastLogin = LocalDateTime.now(),
            ),
        )
    }
}
