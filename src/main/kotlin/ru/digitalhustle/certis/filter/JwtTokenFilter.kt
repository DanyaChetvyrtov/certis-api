package ru.digitalhustle.certis.filter

import io.github.oshai.kotlinlogging.KotlinLogging
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import ru.digitalhustle.certis.service.security.JwtCookieManager
import ru.digitalhustle.certis.service.security.JwtTokenProvider

class JwtTokenFilter(
    private val cookieManager: JwtCookieManager,
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {

    companion object {
        private val log = KotlinLogging.logger {}
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = cookieManager
            .getAccessTokenFromRequest(request)
            ?.removePrefix(BEARER_PREFIX)
            ?.takeIf { it.isNotBlank() }

        if (token == null) {
            filterChain.doFilter(request, response)
            return
        }

        try {
            if (jwtTokenProvider.isValid(token)) {
                SecurityContextHolder.getContext().authentication = jwtTokenProvider.getAuthentication(token)
            }
        } catch (exception: ExpiredJwtException) {
            log.warn { "Expired JWT token for user: ${exception.claims.subject}" }
        } catch (exception: JwtException) {
            log.debug(exception) { "Invalid JWT" }
        }

        filterChain.doFilter(request, response)
    }
}
