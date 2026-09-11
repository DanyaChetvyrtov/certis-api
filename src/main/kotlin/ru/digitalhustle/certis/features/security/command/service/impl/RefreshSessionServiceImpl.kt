package ru.digitalhustle.certis.features.security.command.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.config.properties.JwtProperties
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.features.security.command.repository.RefreshSessionRepository
import ru.digitalhustle.certis.features.security.command.service.RefreshSessionService
import ru.digitalhustle.certis.features.security.exceptions.InvalidTokenException
import ru.digitalhustle.certis.features.security.model.RefreshSession
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class RefreshSessionServiceImpl(
    private val refreshSessionRepository: RefreshSessionRepository,
    private val jwtProperties: JwtProperties,
    private val applicationClock: ApplicationClock,
) : RefreshSessionService {

    @Transactional
    override fun create(userId: UUID): RefreshSession {
        val now = applicationClock.now()

        return refreshSessionRepository.save(
            RefreshSession(
                id = UUID.randomUUID(),
                familyId = UUID.randomUUID(),
                userId = userId,
                expiresAt = now.plus(jwtProperties.refreshDuration),
                usedAt = null,
                revokedAt = null,
                createdAt = now,
            ),
        )
    }

    @Transactional(noRollbackFor = [InvalidTokenException::class])
    override fun rotate(sessionId: UUID, userId: UUID): RefreshSession {
        val now = applicationClock.now()
        val consumedSession = refreshSessionRepository.consume(sessionId, userId, now)
            ?: handleRejectedSession(sessionId, userId, now)

        return refreshSessionRepository.save(
            RefreshSession(
                id = UUID.randomUUID(),
                familyId = consumedSession.familyId,
                userId = consumedSession.userId,
                expiresAt = now.plus(jwtProperties.refreshDuration),
                usedAt = null,
                revokedAt = null,
                createdAt = now,
            ),
        )
    }

    @Transactional
    override fun revokeBySessionId(sessionId: UUID, userId: UUID) {
        val session = refreshSessionRepository.findByIdAndUserId(sessionId, userId) ?: return

        refreshSessionRepository.revokeFamily(session.familyId, userId, applicationClock.now())
    }

    @Transactional
    override fun revokeFamily(familyId: UUID, userId: UUID) {
        refreshSessionRepository.revokeFamily(familyId, userId, applicationClock.now())
    }

    @Transactional
    override fun revokeAll(userId: UUID) {
        refreshSessionRepository.revokeAllByUserId(userId, applicationClock.now())
    }

    private fun handleRejectedSession(sessionId: UUID, userId: UUID, now: OffsetDateTime): Nothing {
        refreshSessionRepository.findByIdAndUserId(sessionId, userId)
            ?.takeIf { it.usedAt != null }
            ?.let { refreshSessionRepository.revokeFamily(it.familyId, userId, now) }

        throw InvalidTokenException(ErrorMessages.INVALID_TOKEN)
    }
}
