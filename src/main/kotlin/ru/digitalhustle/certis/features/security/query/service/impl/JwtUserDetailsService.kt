package ru.digitalhustle.certis.features.security.query.service.impl

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.security.model.JwtDetails
import ru.digitalhustle.certis.features.security.query.service.UserQueryService

@Service
@Transactional(readOnly = true)
class JwtUserDetailsService(
    private val userService: UserQueryService,
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user = try {
            userService.getUserByEmail(email)
        } catch (_: NotFoundException) {
            throw UsernameNotFoundException(ErrorMessages.INVALID_CREDENTIALS)
        }

        return JwtDetails(
            id = user.id,
            username = user.email,
            password = user.passwordHash,
        )
    }
}
