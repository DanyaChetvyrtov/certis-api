package ru.digitalhustle.certis.features.security.command.service

import ru.digitalhustle.certis.features.security.command.model.UserCredentials
import ru.digitalhustle.certis.features.security.model.JwtData
import ru.digitalhustle.certis.features.security.model.User
import java.util.UUID

interface AuthService {

    fun register(userCredentials: UserCredentials): User

    fun login(userCredentials: UserCredentials): JwtData

    fun refreshTokens(refreshToken: String?): JwtData

    fun logout(refreshToken: String?)

    fun revokeSession(userId: UUID, sessionId: UUID)

    fun revokeAllSessions(userId: UUID)
}
