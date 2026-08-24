package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertNotNull
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.constants.SecurityConstants
import ru.digitalhustle.certis.dto.request.LoginRq
import ru.digitalhustle.certis.dto.request.RegisterRq
import ru.digitalhustle.certis.dto.response.ExceptionRs
import ru.digitalhustle.certis.dto.response.SessionsRs
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.provider.SecurityRequestProvider
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@Tag("integration")
class AuthControllerTest : AbstractIntegrationTest() {

    private companion object {
        private const val DEFAULT_CATEGORY_COUNT = 11
    }

    @Test
    fun `should normalize email and register user`() {
        // given
        val denormalizedEmail = "  John@Test.COM "
        val password = "password"

        val request = RegisterRq(
            email = denormalizedEmail,
            password = password,
            passwordConfirmation = password,
        )

        // when
        val response = mvc.perform(
            post(PathConstants.AUTH + PathConstants.REGISTRATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )

        // then
        response.andExpect(status().isCreated)

        val user = requireNotNull(userRepository.findByEmail(SecurityRequestProvider.NORMALIZED_EMAIL))
        val categories = categoryRepository.findAllByUserId(user.id)

        assertThat(user.email).isEqualTo(SecurityRequestProvider.NORMALIZED_EMAIL)
        assertAll(
            { assertThat(categories).hasSize(DEFAULT_CATEGORY_COUNT) },
            {
                assertThat(categories.filter { category -> category.type == CategoryType.EXPENSE }.map { it.name })
                    .containsExactly("Entertainment", "Food", "Health", "Housing", "Other", "Transport", "Utilities")
            },
            {
                assertThat(categories.filter { category -> category.type == CategoryType.INCOME }.map { it.name })
                    .containsExactly("Bonus", "Investment", "Other", "Salary")
            },
            { assertThat(categories).allMatch { category -> category.archivedAt == null } },
        )
    }

    @Test
    fun `should allow only one concurrent registration for normalized email`() {
        // given
        val futureTimeoutSeconds = 30L

        val email = "john@test.com"
        val password = "password"

        val defaultRequest = RegisterRq(
            email = email,
            password = password,
            passwordConfirmation = password,
        )

        val requests = listOf(
            defaultRequest.copy(email = "John@Test.COM"),
            defaultRequest.copy(email = " john@test.com "),
        )

        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(requests.size)

        try {
            val futures = requests.map { request ->
                CompletableFuture.supplyAsync(
                    {
                        start.await()
                        mvc.perform(
                            post(PathConstants.AUTH_REGISTRATION)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request)),
                        ).andReturn().response.status
                    },
                    executor,
                )
            }

            // when
            start.countDown()
            val statuses = futures.map { it.get(futureTimeoutSeconds, TimeUnit.SECONDS) }

            // then
            assertAll(
                {
                    assertThat(statuses).containsExactlyInAnyOrder(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CONFLICT.value(),
                    )
                },
                { assertThat(userRepository.findByEmail(SecurityRequestProvider.NORMALIZED_EMAIL)).isNotNull() },
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `should return 409 when email already exists`() {
        // given
        val email = "john@test.com"
        val password = "password"

        userFixture.createInDb { copy(email = email) }
        val request = RegisterRq(
            email = email,
            password = password,
            passwordConfirmation = password,
        )

        // when
        val response = mvc.perform(
            post(PathConstants.AUTH + PathConstants.REGISTRATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )

        // then
        response
            .andExpect(status().isConflict)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        val errorRs = getBody(response, ExceptionRs::class.java)
        assertAll(
            { assertEquals(HttpStatus.CONFLICT.value(), errorRs.status) },
            { assertEquals(HttpStatus.CONFLICT.reasonPhrase, errorRs.error) },
            { assertEquals("User with such email already exists", errorRs.message) },
        )
    }

    @Test
    fun `should return 400 when password confirmation does not match`() {
        // given
        val email = "john@test.com"
        val password = "password"

        val request = RegisterRq(
            email = email,
            password = password,
            passwordConfirmation = "incorrect-password",
        )

        // when
        val response = mvc.perform(
            post(PathConstants.AUTH + PathConstants.REGISTRATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )

        // then
        response
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        val errorRs = getBody(response, ExceptionRs::class.java)
        assertAll(
            { assertEquals(HttpStatus.BAD_REQUEST.value(), errorRs.status) },
            { assertEquals(HttpStatus.BAD_REQUEST.reasonPhrase, errorRs.error) },
            { assertEquals(ErrorMessages.PASSWORDS_MISMATCH, errorRs.message) },
        )
    }

    @Test
    fun `should login successfully`() {
        // given
        val email = "john@test.com"

        userFixture.createInDb { copy(email = email) }
        val request = LoginRq(email = email, password = "password")

        // when
        val response = performLogin(request)

        // then
        response
            .andExpect(status().isOk)
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))

        val cookies = getHeaders(response, HttpHeaders.SET_COOKIE)
        assertAll(
            {
                assertThat(cookies).anyMatch {
                    it.startsWith("${SecurityConstants.ACCESS_TOKEN_COOKIE}=")
                }
            },
            {
                assertThat(cookies).anyMatch {
                    it.startsWith("${SecurityConstants.REFRESH_TOKEN_COOKIE}=")
                }
            },
        )
    }

    @Test
    fun `should return identical 401 when password is invalid`() {
        // given
        val email = "john@test.com"

        userFixture.createInDb { copy(email = email) }
        val request = LoginRq(email = email, password = "wrong_password")

        // when
        val response = performLogin(request)

        // then
        response
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        val errorRs = getBody(response, ExceptionRs::class.java)
        assertAll(
            { assertEquals(HttpStatus.UNAUTHORIZED.value(), errorRs.status) },
            { assertEquals(HttpStatus.UNAUTHORIZED.reasonPhrase, errorRs.error) },
            { assertEquals(ErrorMessages.INVALID_CREDENTIALS, errorRs.message) },
        )
    }

    @Test
    fun `should return identical 401 when user does not exist`() {
        // given
        val request = LoginRq(email = "unknown@test.com", password = "password")

        // when
        val response = performLogin(request)

        // then
        response
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        val errorRs = getBody(response, ExceptionRs::class.java)
        assertAll(
            { assertEquals(HttpStatus.UNAUTHORIZED.value(), errorRs.status) },
            { assertEquals(HttpStatus.UNAUTHORIZED.reasonPhrase, errorRs.error) },
            { assertEquals(ErrorMessages.INVALID_CREDENTIALS, errorRs.message) },
        )
    }

    @Test
    fun `should rotate refresh token and return both tokens`() {
        // given
        val email = "john@test.com"
        userFixture.createInDb { copy(email = email) }

        val loginRq = LoginRq(
            email = email,
            password = "password",
        )

        val oldCookies = loginAndReturnCookies(loginRq)
        val oldRefreshToken = oldCookies.getValue(SecurityConstants.REFRESH_TOKEN_COOKIE)

        // when
        val response = performRefresh(oldRefreshToken)

        // then
        response
            .andExpect(status().isNoContent)
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))

        val newCookies = responseCookies(
            getHeaders(response, HttpHeaders.SET_COOKIE),
        )

        assertAll(
            {
                assertThat(newCookies).containsKeys(
                    SecurityConstants.ACCESS_TOKEN_COOKIE,
                    SecurityConstants.REFRESH_TOKEN_COOKIE,
                )
            },
            {
                assertThat(newCookies.getValue(SecurityConstants.ACCESS_TOKEN_COOKIE))
                    .isNotBlank()
            },
            {
                assertThat(newCookies.getValue(SecurityConstants.REFRESH_TOKEN_COOKIE))
                    .isNotEqualTo(oldRefreshToken)
            },
        )
    }

    @Test
    fun `should revoke token family when old refresh token is reused`() {
        // given
        val email = "john@test.com"
        userFixture.createInDb { copy(email = email) }

        val loginRq = LoginRq(
            email = email,
            password = "password",
        )

        val oldRefreshToken = loginAndReturnCookies(loginRq)
            .getValue(SecurityConstants.REFRESH_TOKEN_COOKIE)

        val firstRefreshResponse = performRefresh(oldRefreshToken)
            .andExpect(status().isNoContent)

        val newRefreshToken = responseCookies(
            getHeaders(firstRefreshResponse, HttpHeaders.SET_COOKIE),
        ).getValue(SecurityConstants.REFRESH_TOKEN_COOKIE)

        // when: reuse the already rotated token
        performRefresh(oldRefreshToken)
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_TOKEN))

        // then: the entire family must be revoked
        performRefresh(newRefreshToken)
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_TOKEN))
    }

    @Test
    fun `should reject access token at refresh endpoint`() {
        // given
        val email = "john@test.com"
        userFixture.createInDb { copy(email = email) }
        val loginRq = LoginRq(email = email, password = "password")

        val accessToken = loginAndReturnCookies(loginRq).getValue(SecurityConstants.ACCESS_TOKEN_COOKIE)

        // when
        val response = performRefresh(accessToken)

        // then
        response
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        val errorRs = getBody(response, ExceptionRs::class.java)
        assertAll(
            { assertEquals(HttpStatus.UNAUTHORIZED.value(), errorRs.status) },
            { assertEquals(ErrorMessages.INVALID_TOKEN, errorRs.message) },
        )
    }

    @Test
    fun `should return 401 when refresh token is invalid`() {
        // given
        val invalidToken = "invalid_refresh_token"

        // when
        val response = performRefresh(invalidToken)

        // then
        response
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        val errorRs = getBody(response, ExceptionRs::class.java)
        assertAll(
            { assertEquals(HttpStatus.UNAUTHORIZED.value(), errorRs.status) },
            { assertEquals(HttpStatus.UNAUTHORIZED.reasonPhrase, errorRs.error) },
            { assertEquals(ErrorMessages.INVALID_TOKEN, errorRs.message) },
        )
    }

    @Test
    fun `should return json 401 when refresh cookie is missing`() {
        // when
        val response = mvc.perform(post(PathConstants.AUTH_TOKEN))

        // then
        response
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        val errorRs = getBody(response, ExceptionRs::class.java)
        assertAll(
            { assertEquals(HttpStatus.UNAUTHORIZED.value(), errorRs.status) },
            { assertEquals(HttpStatus.UNAUTHORIZED.reasonPhrase, errorRs.error) },
            { assertEquals(ErrorMessages.INVALID_TOKEN, errorRs.message) },
        )
    }

    @Test
    fun `should return json 401 when protected resource has no access token`() {
        // when
        val response = mvc.perform(get(PathConstants.AUTH_SESSIONS))

        // then
        response
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        val errorRs = getBody(response, ExceptionRs::class.java)
        assertAll(
            { assertEquals(HttpStatus.UNAUTHORIZED.value(), errorRs.status) },
            { assertEquals(HttpStatus.UNAUTHORIZED.reasonPhrase, errorRs.error) },
            { assertEquals(ErrorMessages.AUTHENTICATION_REQUIRED, errorRs.message) },
        )
    }

    @Test
    fun `should list active user sessions`() {
        // given
        val email = "john@test.com"
        userFixture.createInDb { copy(email = email) }
        val loginRq = LoginRq(email = email, password = "password")

        val cookies = loginAndReturnCookies(loginRq)

        // when
        val response = mvc.perform(
            get(PathConstants.AUTH_SESSIONS)
                .cookie(
                    Cookie(
                        SecurityConstants.ACCESS_TOKEN_COOKIE,
                        cookies.getValue(SecurityConstants.ACCESS_TOKEN_COOKIE),
                    ),
                ),
        )

        // then
        response
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

        val sessionsRs = getBody(response, SessionsRs::class.java)
        assertEquals(1, sessionsRs.sessions.size)

        val session = sessionsRs.sessions.single()
        assertAll(
            { assertNotNull(session.id) },
            { assertNotNull(session.lastRefreshedAt) },
            { assertNotNull(session.expiresAt) },
        )
    }

    @Test
    fun `should logout current session and remove both cookies`() {
        // given
        val email = "john@test.com"
        userFixture.createInDb { copy(email = email) }
        val loginRq = LoginRq(email = email, password = "password")

        val refreshToken = loginAndReturnCookies(loginRq).getValue(SecurityConstants.REFRESH_TOKEN_COOKIE)

        // when
        val response = mvc.perform(
            post(PathConstants.AUTH_LOGOUT)
                .cookie(Cookie(SecurityConstants.REFRESH_TOKEN_COOKIE, refreshToken)),
        )

        // then
        response
            .andExpect(status().isNoContent)

        val removalCookies = getHeaders(response, HttpHeaders.SET_COOKIE)

        assertAll(
            {
                assertThat(removalCookies).anyMatch {
                    it.startsWith("${SecurityConstants.ACCESS_TOKEN_COOKIE}=;") && "Max-Age=0" in it
                }
            },
            {
                assertThat(removalCookies).anyMatch {
                    it.startsWith("${SecurityConstants.REFRESH_TOKEN_COOKIE}=;") && "Max-Age=0" in it
                }
            },
        )

        performRefresh(refreshToken)
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should revoke selected user session`() {
        // given
        val email = "john@test.com"
        val user = userFixture.createInDb { copy(email = email) }
        val loginRq = LoginRq(email = email, password = "password")

        val currentCookies = loginAndReturnCookies(loginRq)
        val revokedCookies = loginAndReturnCookies(loginRq)

        val revokedPayload = jwtTokenProvider.parseRefreshToken(
            revokedCookies.getValue(SecurityConstants.REFRESH_TOKEN_COOKIE),
        )
        val revokedSession = requireNotNull(
            refreshSessionRepository.findByIdAndUserId(revokedPayload.sessionId, user.id),
        )

        // when
        val response = mvc.perform(
            delete("${PathConstants.AUTH_SESSIONS}/${revokedSession.familyId}")
                .cookie(
                    Cookie(
                        SecurityConstants.ACCESS_TOKEN_COOKIE,
                        currentCookies.getValue(SecurityConstants.ACCESS_TOKEN_COOKIE),
                    ),
                ),
        )

        // then
        response
            .andExpect(status().isNoContent)

        performRefresh(revokedCookies.getValue(SecurityConstants.REFRESH_TOKEN_COOKIE))
            .andExpect(status().isUnauthorized)
        performRefresh(currentCookies.getValue(SecurityConstants.REFRESH_TOKEN_COOKIE))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should revoke all user sessions and remove cookies`() {
        // given
        val email = "john@test.com"
        userFixture.createInDb { copy(email = email) }
        val loginRq = LoginRq(email = email, password = "password")

        val firstCookies = loginAndReturnCookies(loginRq)
        val secondCookies = loginAndReturnCookies(loginRq)

        // when
        val response = mvc.perform(
            delete(PathConstants.AUTH_SESSIONS)
                .cookie(
                    Cookie(
                        SecurityConstants.ACCESS_TOKEN_COOKIE,
                        firstCookies.getValue(SecurityConstants.ACCESS_TOKEN_COOKIE),
                    ),
                ),
        )

        // then
        response
            .andExpect(status().isNoContent)

        assertThat(getHeaders(response, HttpHeaders.SET_COOKIE)).hasSize(2)

        performRefresh(firstCookies.getValue(SecurityConstants.REFRESH_TOKEN_COOKIE))
            .andExpect(status().isUnauthorized)
        performRefresh(secondCookies.getValue(SecurityConstants.REFRESH_TOKEN_COOKIE))
            .andExpect(status().isUnauthorized)
    }

    private fun performLogin(loginRequest: LoginRq): ResultActions =
        mvc.perform(
            post(PathConstants.AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(loginRequest)),
        )

    private fun performRefresh(refreshToken: String): ResultActions =
        mvc.perform(
            post(PathConstants.AUTH_TOKEN)
                .cookie(Cookie(SecurityConstants.REFRESH_TOKEN_COOKIE, refreshToken)),
        )

    private fun loginAndReturnCookies(loginRequest: LoginRq): Map<String, String> {
        val response = performLogin(loginRequest)
            .andExpect(status().isOk)
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))
        val headers = getHeaders(response, HttpHeaders.SET_COOKIE)

        return responseCookies(headers)
    }

    private fun responseCookies(headers: Collection<String>): Map<String, String> =
        headers.associate { header ->
            val nameValue = header.substringBefore(';')
            nameValue.substringBefore('=') to nameValue.substringAfter('=')
        }
}
