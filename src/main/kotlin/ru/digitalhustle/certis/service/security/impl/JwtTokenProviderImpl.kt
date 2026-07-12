package ru.digitalhustle.certis.service.security.impl

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import ru.digitalhustle.certis.config.properties.JwtProperties
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.InvalidTokenException
import ru.digitalhustle.certis.model.security.JwtData
import ru.digitalhustle.certis.service.security.JwtTokenProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtTokenProviderImpl(
    private val jwtProperties: JwtProperties,
    private val userDetailsService: UserDetailsService,
) : JwtTokenProvider {

    companion object {
        private const val ID = "id"
    }

    private lateinit var secretKey: SecretKey

    @PostConstruct
    fun init() {
        secretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    override fun createAccessToken(userId: UUID, email: String): String {
        val claims = Jwts.claims()
            .subject(email)
            .add(ID, userId)
            .build()

        val validity = Instant.now()
            .plus(jwtProperties.accessDuration, ChronoUnit.MINUTES)

        return Jwts.builder()
            .claims(claims)
            .expiration(Date.from(validity))
            .signWith(secretKey)
            .compact()
    }

    override fun createRefreshToken(userId: UUID, email: String): String {
        val claims = Jwts.claims()
            .subject(email)
            .add(ID, userId)
            .build()

        val validity = Instant.now()
            .plus(jwtProperties.refreshDuration, ChronoUnit.DAYS)

        return Jwts.builder()
            .claims(claims)
            .expiration(Date.from(validity))
            .signWith(secretKey)
            .compact()
    }

    override fun refreshUserTokens(refreshToken: String): JwtData {
        if (!isValid(refreshToken)) {
            throw InvalidTokenException(ErrorMessages.TOKEN_EXPIRED)
        }

        val userId = getId(refreshToken)
        val email = getEmail(refreshToken)

        return JwtData(
            id = userId,
            email = email,
            accessToken = createAccessToken(userId, email),
            refreshToken = createRefreshToken(userId, email),
        )
    }

    override fun isValid(token: String): Boolean = getClaims(token).expiration.after(Date())

    override fun getAuthentication(token: String): Authentication {
        val userDetails = userDetailsService.loadUserByUsername(getEmail(token))

        return UsernamePasswordAuthenticationToken(
            userDetails,
            "",
            userDetails.authorities,
        )
    }

    private fun getId(token: String): UUID =
        UUID.fromString(getClaims(token).get(ID, String::class.java))

    private fun getEmail(token: String): String = getClaims(token).subject

    private fun getClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
