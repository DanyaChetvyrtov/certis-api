package ru.digitalhustle.certis.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.model.entity.RefreshSession
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class RefreshSessionRepository(
    private val dsl: DSLContext,
) {

    fun findActiveByUserId(userId: UUID, now: OffsetDateTime): List<RefreshSession> =
        dsl.selectFrom(Tables.REFRESH_SESSIONS)
            .where(Tables.REFRESH_SESSIONS.USER_ID.eq(userId))
            .and(Tables.REFRESH_SESSIONS.USED_AT.isNull)
            .and(Tables.REFRESH_SESSIONS.REVOKED_AT.isNull)
            .and(Tables.REFRESH_SESSIONS.EXPIRES_AT.gt(now))
            .orderBy(Tables.REFRESH_SESSIONS.CREATED_AT.desc())
            .fetchInto(RefreshSession::class.java)

    fun findByIdAndUserId(id: UUID, userId: UUID): RefreshSession? =
        dsl.selectFrom(Tables.REFRESH_SESSIONS)
            .where(Tables.REFRESH_SESSIONS.ID.eq(id))
            .and(Tables.REFRESH_SESSIONS.USER_ID.eq(userId))
            .fetchOneInto(RefreshSession::class.java)

    fun save(refreshSession: RefreshSession): RefreshSession =
        dsl.insertInto(Tables.REFRESH_SESSIONS)
            .set(dsl.newRecord(Tables.REFRESH_SESSIONS, refreshSession))
            .returning()
            .fetchOneInto(RefreshSession::class.java)!!

    fun consume(id: UUID, userId: UUID, usedAt: OffsetDateTime): RefreshSession? =
        dsl.update(Tables.REFRESH_SESSIONS)
            .set(Tables.REFRESH_SESSIONS.USED_AT, usedAt)
            .where(Tables.REFRESH_SESSIONS.ID.eq(id))
            .and(Tables.REFRESH_SESSIONS.USER_ID.eq(userId))
            .and(Tables.REFRESH_SESSIONS.USED_AT.isNull)
            .and(Tables.REFRESH_SESSIONS.REVOKED_AT.isNull)
            .and(Tables.REFRESH_SESSIONS.EXPIRES_AT.gt(usedAt))
            .returning()
            .fetchOneInto(RefreshSession::class.java)

    fun revokeFamily(familyId: UUID, userId: UUID, revokedAt: OffsetDateTime) {
        dsl.update(Tables.REFRESH_SESSIONS)
            .set(Tables.REFRESH_SESSIONS.REVOKED_AT, revokedAt)
            .where(Tables.REFRESH_SESSIONS.FAMILY_ID.eq(familyId))
            .and(Tables.REFRESH_SESSIONS.USER_ID.eq(userId))
            .and(Tables.REFRESH_SESSIONS.REVOKED_AT.isNull)
            .execute()
    }

    fun revokeAllByUserId(userId: UUID, revokedAt: OffsetDateTime) {
        dsl.update(Tables.REFRESH_SESSIONS)
            .set(Tables.REFRESH_SESSIONS.REVOKED_AT, revokedAt)
            .where(Tables.REFRESH_SESSIONS.USER_ID.eq(userId))
            .and(Tables.REFRESH_SESSIONS.REVOKED_AT.isNull)
            .execute()
    }
}
