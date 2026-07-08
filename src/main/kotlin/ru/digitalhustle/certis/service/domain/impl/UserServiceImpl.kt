package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.NotFoundException
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.repository.UserRepository
import ru.digitalhustle.certis.service.domain.UserService
import java.time.LocalDateTime
import java.util.*

@Service
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    override fun getById(id: UUID): User =
        userRepository.findById(id)
            ?: throw NotFoundException.entity("User")

    @Transactional
    override fun create(user: User): User {
        val preparedUser = user.copy(
            id = UUID.randomUUID(),
            createdAt = LocalDateTime.now(),
            preferredCurrency = user.preferredCurrency ?: Currency.RUB,
        )

        return userRepository.save(preparedUser)
    }

    @Transactional
    override fun update(user: User): User {
        val dbUser = userRepository.findById(user.id)
            ?: throw NotFoundException.entity("User")

        val updatedUser = user.copy(
            id = dbUser.id,
            createdAt = LocalDateTime.now(),
        )

        return userRepository.save(updatedUser)
    }

    override fun deleteById(id: UUID) {
        userRepository.deleteById(id)
    }
}