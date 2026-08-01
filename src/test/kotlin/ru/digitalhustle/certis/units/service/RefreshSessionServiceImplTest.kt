package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.config.properties.JwtProperties
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.InvalidTokenException
import ru.digitalhustle.certis.model.entity.RefreshSession
import ru.digitalhustle.certis.repository.RefreshSessionRepository
import ru.digitalhustle.certis.service.domain.impl.RefreshSessionServiceImpl
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class RefreshSessionServiceImplTest {

    private val repository = mock(RefreshSessionRepository::class.java)
    private val clock = Clock.fixed(NOW.toInstant(), ZoneOffset.UTC)
    private val service = RefreshSessionServiceImpl(
        refreshSessionRepository = repository,
        jwtProperties = JwtProperties(
            secret = "01234567890123456789012345678901",
            accessDuration = ACCESS_DURATION,
            refreshDuration = REFRESH_DURATION,
        ),
        clock = clock,
    )

    private companion object {
        private val ACCESS_DURATION = Duration.ofMinutes(30)
        private val REFRESH_DURATION = Duration.ofDays(7)
        private val NOW = OffsetDateTime.ofInstant(
            Instant.parse("2026-08-01T12:00:00Z"),
            ZoneOffset.UTC,
        )
    }

    @Test
    fun `should return active sessions for user`() {
        // given
        val userId = UUID.randomUUID()
        val sessions = listOf(createSession(usedAt = null).copy(userId = userId))
        `when`(repository.findActiveByUserId(userId, NOW)).thenReturn(sessions)

        // when
        val result = service.getActiveByUserId(userId)

        // then
        assertThat(result).isEqualTo(sessions)
    }

    @Test
    fun `should create initial refresh session`() {
        // given
        val userId = UUID.randomUUID()
        `when`(repository.save(anyRefreshSession()))
            .thenAnswer { it.arguments[0] as RefreshSession }

        // when
        val session = service.create(userId)

        // then
        assertAll(
            { assertThat(session.userId).isEqualTo(userId) },
            { assertThat(session.id).isNotNull() },
            { assertThat(session.familyId).isNotNull() },
            { assertThat(session.createdAt).isEqualTo(NOW) },
            { assertThat(session.expiresAt).isEqualTo(NOW.plus(REFRESH_DURATION)) },
            { assertThat(session.usedAt).isNull() },
            { assertThat(session.revokedAt).isNull() },
        )
    }

    @Test
    fun `should consume current session and create successor in same family`() {
        // given
        val currentSession = createSession(usedAt = NOW)

        `when`(repository.consume(currentSession.id, currentSession.userId, NOW))
            .thenReturn(currentSession)
        `when`(repository.save(anyRefreshSession()))
            .thenAnswer { it.arguments[0] as RefreshSession }

        // when
        val successor = service.rotate(currentSession.id, currentSession.userId)

        // then
        assertAll(
            { assertThat(successor.id).isNotEqualTo(currentSession.id) },
            { assertThat(successor.familyId).isEqualTo(currentSession.familyId) },
            { assertThat(successor.userId).isEqualTo(currentSession.userId) },
            { assertThat(successor.createdAt).isEqualTo(NOW) },
            { assertThat(successor.usedAt).isNull() },
            { assertThat(successor.revokedAt).isNull() },
        )
    }

    @Test
    fun `should revoke token family when consumed session is reused`() {
        // given
        val reusedSession = createSession(usedAt = NOW.minusSeconds(1))

        `when`(repository.consume(reusedSession.id, reusedSession.userId, NOW))
            .thenReturn(null)
        `when`(repository.findByIdAndUserId(reusedSession.id, reusedSession.userId))
            .thenReturn(reusedSession)

        // when, then
        assertThatThrownBy {
            service.rotate(reusedSession.id, reusedSession.userId)
        }.isInstanceOf(InvalidTokenException::class.java)
            .hasMessage(ErrorMessages.INVALID_TOKEN)

        verify(repository).revokeFamily(reusedSession.familyId, reusedSession.userId, NOW)
    }

    @Test
    fun `should reject unknown session without revoking another family`() {
        // given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(repository.consume(sessionId, userId, NOW)).thenReturn(null)
        `when`(repository.findByIdAndUserId(sessionId, userId)).thenReturn(null)

        // when, then
        assertThatThrownBy {
            service.rotate(sessionId, userId)
        }.isInstanceOf(InvalidTokenException::class.java)

        verify(repository).consume(sessionId, userId, NOW)
        verify(repository).findByIdAndUserId(sessionId, userId)
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `should revoke family by token session id`() {
        // given
        val session = createSession(usedAt = null)
        `when`(repository.findByIdAndUserId(session.id, session.userId)).thenReturn(session)

        // when
        service.revokeBySessionId(session.id, session.userId)

        // then
        verify(repository).revokeFamily(session.familyId, session.userId, NOW)
    }

    @Test
    fun `should ignore unknown token session during logout`() {
        // given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        `when`(repository.findByIdAndUserId(sessionId, userId)).thenReturn(null)

        // when
        service.revokeBySessionId(sessionId, userId)

        // then
        verify(repository).findByIdAndUserId(sessionId, userId)
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `should revoke selected family only for its owner`() {
        // given
        val familyId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        // when
        service.revokeFamily(familyId, userId)

        // then
        verify(repository).revokeFamily(familyId, userId, NOW)
    }

    @Test
    fun `should revoke all sessions for user`() {
        // given
        val userId = UUID.randomUUID()

        // when
        service.revokeAll(userId)

        // then
        verify(repository).revokeAllByUserId(userId, NOW)
    }

    private fun anyRefreshSession(): RefreshSession {
        any(RefreshSession::class.java)
        return createSession(usedAt = null)
    }

    private fun createSession(usedAt: OffsetDateTime?): RefreshSession =
        RefreshSession(
            id = UUID.randomUUID(),
            familyId = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            expiresAt = NOW.plus(REFRESH_DURATION),
            usedAt = usedAt,
            revokedAt = null,
            createdAt = NOW.minusDays(1),
        )
}
