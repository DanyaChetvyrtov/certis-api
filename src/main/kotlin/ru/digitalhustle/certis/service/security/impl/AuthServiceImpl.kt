package ru.digitalhustle.certis.service.security.impl

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.PasswordsDoNotMatchException
import ru.digitalhustle.certis.model.UserCredentials
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.model.security.JwtData
import ru.digitalhustle.certis.service.domain.UserService
import ru.digitalhustle.certis.service.security.AuthService
import ru.digitalhustle.certis.service.security.JwtTokenProvider

@Service
class AuthServiceImpl(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authenticationManager: AuthenticationManager,
) : AuthService {

    override fun register(userCredentials: UserCredentials): User {
        if (userCredentials.password != userCredentials.passwordConfirmation) {
            throw PasswordsDoNotMatchException(ErrorMessages.PASSWORDS_MISMATCH)
        }

        val encodedPassword = passwordEncoder.encode(userCredentials.password)
        return userService.save(userCredentials.email, encodedPassword)
    }

    override fun login(userCredentials: UserCredentials): JwtData {
        val dbUser = userService.getUserByEmail(userCredentials.email)

        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(userCredentials.email, userCredentials.password),
        )

        userService.updateLastLogin(dbUser.id)

        return JwtData(
            id = dbUser.id,
            email = dbUser.email,
            accessToken = jwtTokenProvider.createAccessToken(
                dbUser.id,
                dbUser.email,
            ),
            refreshToken = jwtTokenProvider.createRefreshToken(
                dbUser.id,
                dbUser.email,
            ),
        )
    }

    override fun refreshAccess(refreshToken: String): JwtData = jwtTokenProvider.refreshUserTokens(refreshToken)

    override fun refreshTokens(refreshToken: String): JwtData = jwtTokenProvider.refreshUserTokens(refreshToken)
}
