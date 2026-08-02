package ru.digitalhustle.certis.units.filter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import ru.digitalhustle.certis.config.properties.AuthRateLimitProperties
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.filter.AuthRateLimitFilter
import ru.digitalhustle.certis.producer.ExceptionResponseProducer
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class AuthRateLimitFilterTest {

    private val objectMapper = jacksonObjectMapper().findAndRegisterModules()
    private val filter = AuthRateLimitFilter(
        properties = AuthRateLimitProperties(
            cacheMaximumSize = 100,
            cacheExpireAfter = Duration.ofMinutes(5),
            login = rule(capacity = 1),
            registration = rule(capacity = 1),
            refresh = rule(capacity = 1),
        ),
        exceptionResponseProducer = ExceptionResponseProducer(
            Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC),
        ),
        objectMapper = objectMapper,
    )

    @Test
    fun `should return 429 when login limit is exceeded`() {
        // given
        val firstResponse = MockHttpServletResponse()
        filter.doFilter(loginRequest(), firstResponse, MockFilterChain())

        val limitedResponse = MockHttpServletResponse()

        // when
        filter.doFilter(loginRequest(), limitedResponse, MockFilterChain())

        // then
        val body = objectMapper.readTree(limitedResponse.contentAsString)

        assertThat(firstResponse.status).isEqualTo(HttpStatus.OK.value())
        assertThat(firstResponse.getHeader("X-RateLimit-Limit")).isEqualTo("1")
        assertThat(firstResponse.getHeader("X-RateLimit-Remaining")).isEqualTo("0")
        assertThat(limitedResponse.status).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value())
        assertThat(limitedResponse.getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank()
        assertThat(body["status"].asInt()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value())
        assertThat(body["message"].asText()).isEqualTo(ErrorMessages.TOO_MANY_REQUESTS)
    }

    @Test
    fun `should maintain separate buckets for different client addresses`() {
        // when
        val firstResponse = MockHttpServletResponse()
        filter.doFilter(loginRequest("192.0.2.1"), firstResponse, MockFilterChain())

        val secondResponse = MockHttpServletResponse()
        filter.doFilter(loginRequest("192.0.2.2"), secondResponse, MockFilterChain())

        // then
        assertThat(firstResponse.status).isEqualTo(HttpStatus.OK.value())
        assertThat(secondResponse.status).isEqualTo(HttpStatus.OK.value())
    }

    @Test
    fun `should maintain separate buckets for different authentication operations`() {
        // when
        val loginResponse = MockHttpServletResponse()
        filter.doFilter(loginRequest(), loginResponse, MockFilterChain())

        val registrationResponse = MockHttpServletResponse()
        filter.doFilter(
            authRequest(PathConstants.AUTH + PathConstants.REGISTRATION),
            registrationResponse,
            MockFilterChain(),
        )

        val refreshResponse = MockHttpServletResponse()
        filter.doFilter(authRequest(PathConstants.AUTH_TOKEN), refreshResponse, MockFilterChain())

        // then
        assertThat(loginResponse.status).isEqualTo(HttpStatus.OK.value())
        assertThat(registrationResponse.status).isEqualTo(HttpStatus.OK.value())
        assertThat(refreshResponse.status).isEqualTo(HttpStatus.OK.value())
    }

    @Test
    fun `should not limit non-authentication endpoints`() {
        // given
        val request = MockHttpServletRequest("GET", PathConstants.PROFILES)
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, MockFilterChain())

        // then
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        assertThat(response.getHeader("X-RateLimit-Limit")).isNull()
    }

    private fun loginRequest(remoteAddress: String = "192.0.2.1") =
        authRequest(PathConstants.AUTH, remoteAddress)

    private fun authRequest(path: String, remoteAddress: String = "192.0.2.1") =
        MockHttpServletRequest("POST", path).apply {
            remoteAddr = remoteAddress
        }

    private fun rule(capacity: Long) =
        AuthRateLimitProperties.Rule(
            capacity = capacity,
            refillPeriod = Duration.ofMinutes(1),
        )
}
