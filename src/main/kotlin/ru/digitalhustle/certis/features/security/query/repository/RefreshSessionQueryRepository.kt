package ru.digitalhustle.certis.features.security.query.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.security.model.RefreshSession
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class RefreshSessionQueryRepository(
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
}
