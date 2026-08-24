package ru.digitalhustle.certis.service.security

import org.springframework.security.core.Authentication
import ru.digitalhustle.certis.model.security.RefreshTokenPayload
import java.time.Instant
import java.util.UUID

interface JwtTokenProvider {

    fun createAccessToken(userId: UUID, email: String): String

    fun createRefreshToken(
        userId: UUID,
        email: String,
        sessionId: UUID,
        expiresAt: Instant,
    ): String

    fun parseRefreshToken(refreshToken: String): RefreshTokenPayload

    fun isValidAccessToken(token: String): Boolean

    fun getAuthentication(token: String): Authentication
}
