package ru.digitalhustle.certis.service.security

import org.springframework.security.core.Authentication
import ru.digitalhustle.certis.model.security.JwtData
import java.util.UUID

interface JwtTokenProvider {

    fun createAccessToken(userId: UUID, email: String): String

    fun createRefreshToken(userId: UUID, email: String): String

    fun refreshUserTokens(refreshToken: String): JwtData

    fun isValid(token: String): Boolean

    fun getAuthentication(token: String): Authentication
}
