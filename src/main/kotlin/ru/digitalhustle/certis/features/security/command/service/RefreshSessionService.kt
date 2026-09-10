package ru.digitalhustle.certis.features.security.command.service

import ru.digitalhustle.certis.features.security.model.RefreshSession
import java.util.UUID

interface RefreshSessionService {

    fun create(userId: UUID): RefreshSession

    fun rotate(sessionId: UUID, userId: UUID): RefreshSession

    fun revokeBySessionId(sessionId: UUID, userId: UUID)

    fun revokeFamily(familyId: UUID, userId: UUID)

    fun revokeAll(userId: UUID)
}
