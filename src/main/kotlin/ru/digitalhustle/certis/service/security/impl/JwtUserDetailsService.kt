package ru.digitalhustle.certis.service.security.impl

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.domain.UserService

@Service
class JwtUserDetailsService(
    private val userService: UserService,
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
