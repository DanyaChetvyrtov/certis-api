package ru.digitalhustle.certis.service.security.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.InvalidTokenException
import ru.digitalhustle.certis.exception.custom.MissedTokenException
import ru.digitalhustle.certis.exception.custom.PasswordsDoNotMatchException
import ru.digitalhustle.certis.model.entity.RefreshSession
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.model.security.JwtData
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.model.security.UserCredentials
import ru.digitalhustle.certis.service.domain.CategoryService
import ru.digitalhustle.certis.service.domain.RefreshSessionService
import ru.digitalhustle.certis.service.domain.UserService
import ru.digitalhustle.certis.service.security.AuthService
import ru.digitalhustle.certis.service.security.JwtTokenProvider
import ru.digitalhustle.certis.util.normalizer.EmailNormalizer
import java.util.UUID

@Service
class AuthServiceImpl(
    private val userService: UserService,
    private val categoryService: CategoryService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshSessionService: RefreshSessionService,
    private val authenticationManager: AuthenticationManager,
) : AuthService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun getSessions(userId: UUID): List<RefreshSession> =
        refreshSessionService.getActiveByUserId(userId)

    @Transactional
    override fun register(userCredentials: UserCredentials): User {
        if (userCredentials.password != userCredentials.passwordConfirmation) {
            throw PasswordsDoNotMatchException(ErrorMessages.PASSWORDS_MISMATCH)
        }

        val encodedPassword = passwordEncoder.encode(userCredentials.password)
        val user = userService.save(EmailNormalizer.normalize(userCredentials.email), encodedPassword)

        categoryService.createDefaults(user.id)

        return user
    }

    override fun login(userCredentials: UserCredentials): JwtData {
        val normalizedEmail = EmailNormalizer.normalize(userCredentials.email)
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(normalizedEmail, userCredentials.password),
        )
        val principal = authentication.principal as JwtDetails

        userService.updateLastLogin(principal.id)

        return issueTokens(principal.id, principal.username)
    }

    override fun refreshTokens(refreshToken: String?): JwtData {
        val token = refreshToken?.takeIf(String::isNotBlank)
            ?: throw MissedTokenException(ErrorMessages.INVALID_TOKEN)
        val payload = jwtTokenProvider.parseRefreshToken(token)
        val newSession = refreshSessionService.rotate(payload.sessionId, payload.userId)

        return issueTokens(payload.userId, payload.email, newSession)
    }

    override fun logout(refreshToken: String?) {
        val token = refreshToken?.takeIf(String::isNotBlank) ?: return

        try {
            val payload = jwtTokenProvider.parseRefreshToken(token)
            refreshSessionService.revokeBySessionId(payload.sessionId, payload.userId)
        } catch (exception: InvalidTokenException) {
            log.debug(exception) { "Logout requested with an invalid refresh token" }
        }
    }

    override fun revokeSession(userId: UUID, sessionId: UUID) {
        refreshSessionService.revokeFamily(sessionId, userId)
    }

    override fun revokeAllSessions(userId: UUID) {
        refreshSessionService.revokeAll(userId)
    }

    private fun issueTokens(
        userId: UUID,
        email: String,
        refreshSession: RefreshSession = refreshSessionService.create(userId),
    ): JwtData =
        JwtData(
            id = userId,
            email = email,
            accessToken = jwtTokenProvider.createAccessToken(userId, email),
            refreshToken = jwtTokenProvider.createRefreshToken(
                userId = userId,
                email = email,
                sessionId = refreshSession.id,
                expiresAt = refreshSession.expiresAt.toInstant(),
            ),
        )
}
