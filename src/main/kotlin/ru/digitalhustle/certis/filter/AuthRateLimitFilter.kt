package ru.digitalhustle.certis.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.digitalhustle.certis.config.properties.AuthRateLimitProperties
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.provider.ExceptionResponseProvider
import kotlin.math.ceil

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class AuthRateLimitFilter(
    private val properties: AuthRateLimitProperties,
    private val exceptionResponseProvider: ExceptionResponseProvider,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    companion object {
        private const val LOGIN = "login"
        private const val REGISTRATION = "registration"
        private const val REFRESH = "refresh"
        private const val NANOS_IN_SECOND = 1_000_000_000.0
    }

    private val rulesByPath = mapOf(
        PathConstants.AUTH to NamedRule(LOGIN, properties.login),
        "${PathConstants.AUTH}${PathConstants.REGISTRATION}" to NamedRule(REGISTRATION, properties.registration),
        PathConstants.AUTH_TOKEN to NamedRule(REFRESH, properties.refresh),
    )

    private val buckets: Cache<String, Bucket> = Caffeine.newBuilder()
        .maximumSize(properties.cacheMaximumSize)
        .expireAfterAccess(properties.cacheExpireAfter)
        .build()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != HttpMethod.POST.name() || request.requestURI !in rulesByPath

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val namedRule = requireNotNull(rulesByPath[request.requestURI])
        val bucket = buckets.get("${namedRule.name}:${request.remoteAddr}") {
            createBucket(namedRule.rule)
        }
        val probe = bucket.tryConsumeAndReturnRemaining(1)

        response.setHeader(SecurityConstants.RATE_LIMIT_HEADER, namedRule.rule.capacity.toString())
        response.setHeader(SecurityConstants.RATE_LIMIT_REMAINING_HEADER, probe.remainingTokens.toString())

        if (!probe.isConsumed) {
            val retryAfter = ceil(probe.nanosToWaitForRefill / NANOS_IN_SECOND)
                .toLong()
                .coerceAtLeast(1)

            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = Charsets.UTF_8.name()
            response.setHeader(HttpHeaders.RETRY_AFTER, retryAfter.toString())

            objectMapper.writeValue(
                response.writer,
                exceptionResponseProvider.createResponse(
                    status = HttpStatus.TOO_MANY_REQUESTS,
                    message = ErrorMessages.TOO_MANY_REQUESTS,
                ),
            )
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun createBucket(rule: AuthRateLimitProperties.Rule): Bucket {
        val bandwidth = Bandwidth.builder()
            .capacity(rule.capacity)
            .refillGreedy(rule.capacity, rule.refillPeriod)
            .build()

        return Bucket.builder()
            .addLimit(bandwidth)
            .build()
    }

    private data class NamedRule(
        val name: String,
        val rule: AuthRateLimitProperties.Rule,
    )
}
