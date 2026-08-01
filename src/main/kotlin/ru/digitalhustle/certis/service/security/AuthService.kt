package ru.digitalhustle.certis.service.security

import ru.digitalhustle.certis.model.entity.RefreshSession
import ru.digitalhustle.certis.model.entity.User
import ru.digitalhustle.certis.model.security.JwtData
import ru.digitalhustle.certis.model.security.UserCredentials
import java.util.UUID

interface AuthService {

    fun getSessions(userId: UUID): List<RefreshSession>

    fun register(userCredentials: UserCredentials): User

    fun login(userCredentials: UserCredentials): JwtData

    fun refreshTokens(refreshToken: String?): JwtData

    fun logout(refreshToken: String?)

    fun revokeSession(userId: UUID, sessionId: UUID)

    fun revokeAllSessions(userId: UUID)
}
