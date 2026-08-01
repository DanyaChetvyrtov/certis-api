package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.entity.RefreshSession
import java.util.UUID

interface RefreshSessionService {

    fun getActiveByUserId(userId: UUID): List<RefreshSession>

    fun create(userId: UUID): RefreshSession

    fun rotate(sessionId: UUID, userId: UUID): RefreshSession

    fun revokeBySessionId(sessionId: UUID, userId: UUID)

    fun revokeFamily(familyId: UUID, userId: UUID)

    fun revokeAll(userId: UUID)
}
