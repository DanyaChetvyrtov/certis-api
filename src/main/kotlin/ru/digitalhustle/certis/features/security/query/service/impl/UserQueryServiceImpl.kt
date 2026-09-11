package ru.digitalhustle.certis.features.security.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.security.model.User
import ru.digitalhustle.certis.features.security.query.repository.UserQueryRepository
import ru.digitalhustle.certis.features.security.query.service.UserQueryService
import ru.digitalhustle.certis.util.normalizer.EmailNormalizer
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserQueryServiceImpl(
    private val userRepository: UserQueryRepository,
) : UserQueryService {

    override fun getUserById(id: UUID): User =
        userRepository.findById(id)
            ?: throw NotFoundException.entity("User")

    override fun getUserByEmail(email: String): User =
        userRepository.findByEmail(EmailNormalizer.normalize(email))
            ?: throw NotFoundException.entity("User")

    override fun getPreferredCurrency(id: UUID): Currency =
        userRepository.findPreferredCurrency(id)
            ?: throw NotFoundException.entity("User")
}
