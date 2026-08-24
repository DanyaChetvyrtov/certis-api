package ru.digitalhustle.certis.service.security.impl

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.config.properties.JwtProperties
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.enums.JwtTokenType
import ru.digitalhustle.certis.exception.custom.InvalidTokenException
import ru.digitalhustle.certis.model.security.RefreshTokenPayload
import ru.digitalhustle.certis.service.security.JwtTokenProvider
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtTokenProviderImpl(
    private val jwtProperties: JwtProperties,
    private val userDetailsService: UserDetailsService,
    private val clock: Clock,
) : JwtTokenProvider {

    companion object {
        private const val ID = "id"
        private const val TYPE = "type"
    }

    private lateinit var secretKey: SecretKey

    @PostConstruct
    fun init() {
        secretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    override fun createAccessToken(userId: UUID, email: String): String {
        val issuedAt = Instant.now(clock)
        val claims = Jwts.claims()
            .subject(email)
            .add(ID, userId)
            .add(TYPE, JwtTokenType.ACCESS.name)
            .build()

        val validity = issuedAt.plus(jwtProperties.accessDuration)

        return Jwts.builder()
            .claims(claims)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(validity))
            .signWith(secretKey)
            .compact()
    }

    override fun createRefreshToken(
        userId: UUID,
        email: String,
        sessionId: UUID,
        expiresAt: Instant,
    ): String {
        val issuedAt = Instant.now(clock)
        val claims = Jwts.claims()
            .subject(email)
            .add(ID, userId)
            .add(TYPE, JwtTokenType.REFRESH.name)
            .build()

        return Jwts.builder()
            .claims(claims)
            .id(sessionId.toString())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey)
            .compact()
    }

    override fun parseRefreshToken(refreshToken: String): RefreshTokenPayload =
        try {
            val claims = getClaims(refreshToken, JwtTokenType.REFRESH)

            RefreshTokenPayload(
                sessionId = parseUuidClaim(claims.id),
                userId = getUserId(claims),
                email = claims.subject ?: throw InvalidTokenException(ErrorMessages.INVALID_TOKEN),
            )
        } catch (_: JwtException) {
            throw InvalidTokenException(ErrorMessages.INVALID_TOKEN)
        } catch (_: IllegalArgumentException) {
            throw InvalidTokenException(ErrorMessages.INVALID_TOKEN)
        }

    override fun isValidAccessToken(token: String): Boolean =
        runCatching {
            getClaims(token, JwtTokenType.ACCESS)
        }.isSuccess

    override fun getAuthentication(token: String): Authentication {
        val claims = getClaims(token, JwtTokenType.ACCESS)
        val userDetails = userDetailsService.loadUserByUsername(claims.subject)

        return UsernamePasswordAuthenticationToken(
            userDetails,
            "",
            userDetails.authorities,
        )
    }

    private fun getUserId(claims: Claims): UUID = parseUuidClaim(claims.get(ID, String::class.java))

    private fun parseUuidClaim(value: String?): UUID =
        value?.let(UUID::fromString)
            ?: throw InvalidTokenException(ErrorMessages.INVALID_TOKEN)

    private fun getClaims(token: String, expectedType: JwtTokenType): Claims {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .clock { Date.from(Instant.now(clock)) }
            .build()
            .parseSignedClaims(token)
            .payload

        if (claims.get(TYPE, String::class.java) != expectedType.name) {
            throw InvalidTokenException(ErrorMessages.INVALID_TOKEN)
        }

        return claims
    }
}
