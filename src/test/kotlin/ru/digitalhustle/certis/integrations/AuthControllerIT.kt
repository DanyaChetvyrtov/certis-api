package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.request.LoginRq
import ru.digitalhustle.certis.dto.request.RegisterRq
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AuthControllerIT : AbstractIntegrationTest() {

    @Test
    fun `should register user`() {
        // given
        val request = RegisterRq(
            email = "  John@Test.COM ",
            password = "password",
            passwordConfirmation = "password",
        )

        // when
        mvc.perform(
            post(PathConstants.AUTH + PathConstants.REGISTRATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isCreated)

        val user = requireNotNull(userRepository.findByEmail("john@test.com"))
        assertThat(user.email).isEqualTo("john@test.com")
    }

    @Test
    fun `should allow only one concurrent registration for normalized email`() {
        // given
        val requests = listOf(
            RegisterRq("John@Test.COM", "password", "password"),
            RegisterRq(" john@test.com ", "password", "password"),
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
            val statuses = futures.map { it.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS) }

            // then
            assertThat(statuses).containsExactlyInAnyOrder(
                HttpStatus.CREATED.value(),
                HttpStatus.CONFLICT.value(),
            )
            assertThat(userRepository.findByEmail("john@test.com")).isNotNull()
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `should return 409 when email already exists`() {
        // given
        userFixture.create { copy(email = "john@test.com") }
        val request = RegisterRq(
            email = "john@test.com",
            password = "password",
            passwordConfirmation = "password",
        )

        // when
        mvc.perform(
            post(PathConstants.AUTH + PathConstants.REGISTRATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("User with such email already exists"))
    }

    @Test
    fun `should return 400 when password confirmation does not match`() {
        // given
        val request = RegisterRq(
            email = "john@test.com",
            password = "password",
            passwordConfirmation = "different_password",
        )

        // when
        mvc.perform(
            post(PathConstants.AUTH + PathConstants.REGISTRATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(ErrorMessages.PASSWORDS_MISMATCH))
    }

    @Test
    fun `should login successfully`() {
        // given
        userFixture.create()

        // when
        val result = performLogin()

        // then
        result
            .andExpect(status().isOk)
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))

        val cookies = result.andReturn().response.getHeaders(HttpHeaders.SET_COOKIE)
        assertThat(cookies).anyMatch { it.startsWith("access_token=") }
        assertThat(cookies).anyMatch { it.startsWith("refresh_token=") }
    }

    @Test
    fun `should return identical 401 when password is invalid`() {
        // given
        userFixture.create()
        val request = LoginRq(email = "user@test.com", password = "wrong_password")

        // when
        mvc.perform(
            post(PathConstants.AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_CREDENTIALS))
    }

    @Test
    fun `should return identical 401 when user does not exist`() {
        // given
        val request = LoginRq(email = "unknown@test.com", password = "password")

        // when
        mvc.perform(
            post(PathConstants.AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )
            // then
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_CREDENTIALS))
    }

    @Test
    fun `should rotate both tokens`() {
        // given
        userFixture.create()
        val oldRefreshToken = loginCookies().getValue(REFRESH_COOKIE)

        // when
        val result = performRefresh(oldRefreshToken)

        // then
        result
            .andExpect(status().isNoContent)
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))

        val cookies = responseCookies(result.andReturn().response.getHeaders(HttpHeaders.SET_COOKIE))
        assertThat(cookies).containsKeys(ACCESS_COOKIE, REFRESH_COOKIE)
        assertThat(cookies.getValue(REFRESH_COOKIE)).isNotEqualTo(oldRefreshToken)
    }

    @Test
    fun `should revoke token family when old refresh token is reused`() {
        // given
        userFixture.create()
        val oldRefreshToken = loginCookies().getValue(REFRESH_COOKIE)
        val firstRefresh = performRefresh(oldRefreshToken)
            .andExpect(status().isNoContent)
            .andReturn()
        val newRefreshToken = responseCookies(
            firstRefresh.response.getHeaders(HttpHeaders.SET_COOKIE),
        ).getValue(REFRESH_COOKIE)

        // when, then
        performRefresh(oldRefreshToken)
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_TOKEN))

        performRefresh(newRefreshToken)
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_TOKEN))
    }

    @Test
    fun `should reject access token at refresh endpoint`() {
        // given
        userFixture.create()
        val accessToken = loginCookies().getValue(ACCESS_COOKIE)

        // when
        performRefresh(accessToken)
            // then
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_TOKEN))
    }

    @Test
    fun `should return 401 when refresh token is invalid`() {
        // when
        performRefresh("invalid_refresh_token")
            // then
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
            .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_TOKEN))
    }

    @Test
    fun `should return json 401 when refresh cookie is missing`() {
        mvc.perform(post(PathConstants.AUTH_TOKEN))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
            .andExpect(jsonPath("$.error").value(HttpStatus.UNAUTHORIZED.reasonPhrase))
            .andExpect(jsonPath("$.message").value(ErrorMessages.INVALID_TOKEN))
    }

    @Test
    fun `should return json 401 when protected resource has no access token`() {
        mvc.perform(get(PathConstants.AUTH_SESSIONS))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
            .andExpect(jsonPath("$.error").value(HttpStatus.UNAUTHORIZED.reasonPhrase))
            .andExpect(jsonPath("$.message").value(ErrorMessages.AUTHENTICATION_REQUIRED))
    }

    @Test
    fun `should list active user sessions`() {
        // given
        userFixture.create()
        val cookies = loginCookies()

        // when
        mvc.perform(
            get(PathConstants.AUTH_SESSIONS)
                .cookie(Cookie(ACCESS_COOKIE, cookies.getValue(ACCESS_COOKIE))),
        )
            // then
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sessions.length()").value(1))
            .andExpect(jsonPath("$.sessions[0].id").isNotEmpty())
            .andExpect(jsonPath("$.sessions[0].lastRefreshedAt").isNotEmpty())
            .andExpect(jsonPath("$.sessions[0].expiresAt").isNotEmpty())
    }

    @Test
    fun `should logout current session and remove both cookies`() {
        // given
        userFixture.create()
        val refreshToken = loginCookies().getValue(REFRESH_COOKIE)

        // when
        val result = mvc.perform(
            post(PathConstants.AUTH_LOGOUT)
                .cookie(Cookie(REFRESH_COOKIE, refreshToken)),
        )
            // then
            .andExpect(status().isNoContent)
            .andReturn()

        val removalCookies = result.response.getHeaders(HttpHeaders.SET_COOKIE)
        assertThat(removalCookies).anyMatch { it.startsWith("$ACCESS_COOKIE=;") && "Max-Age=0" in it }
        assertThat(removalCookies).anyMatch { it.startsWith("$REFRESH_COOKIE=;") && "Max-Age=0" in it }

        performRefresh(refreshToken)
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should revoke selected user session`() {
        // given
        val user = userFixture.create()
        val currentCookies = loginCookies()
        val revokedCookies = loginCookies()
        val revokedPayload = jwtTokenProvider.parseRefreshToken(revokedCookies.getValue(REFRESH_COOKIE))
        val revokedSession = requireNotNull(
            refreshSessionRepository.findByIdAndUserId(revokedPayload.sessionId, user.id),
        )

        // when
        mvc.perform(
            delete("${PathConstants.AUTH_SESSIONS}/${revokedSession.familyId}")
                .cookie(Cookie(ACCESS_COOKIE, currentCookies.getValue(ACCESS_COOKIE))),
        )
            // then
            .andExpect(status().isNoContent)

        performRefresh(revokedCookies.getValue(REFRESH_COOKIE))
            .andExpect(status().isUnauthorized)
        performRefresh(currentCookies.getValue(REFRESH_COOKIE))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should revoke all user sessions and remove cookies`() {
        // given
        userFixture.create()
        val firstCookies = loginCookies()
        val secondCookies = loginCookies()

        // when
        val result = mvc.perform(
            delete(PathConstants.AUTH_SESSIONS)
                .cookie(Cookie(ACCESS_COOKIE, firstCookies.getValue(ACCESS_COOKIE))),
        )
            // then
            .andExpect(status().isNoContent)
            .andReturn()

        assertThat(result.response.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2)
        performRefresh(firstCookies.getValue(REFRESH_COOKIE))
            .andExpect(status().isUnauthorized)
        performRefresh(secondCookies.getValue(REFRESH_COOKIE))
            .andExpect(status().isUnauthorized)
    }

    private fun performLogin() =
        mvc.perform(
            post(PathConstants.AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        LoginRq(email = "user@test.com", password = "password"),
                    ),
                ),
        )

    private fun loginCookies(): Map<String, String> =
        responseCookies(
            performLogin()
                .andExpect(status().isOk)
                .andReturn()
                .response
                .getHeaders(HttpHeaders.SET_COOKIE),
        )

    private fun performRefresh(refreshToken: String) =
        mvc.perform(
            post(PathConstants.AUTH_TOKEN)
                .cookie(Cookie(REFRESH_COOKIE, refreshToken)),
        )

    private fun responseCookies(headers: Collection<String>): Map<String, String> =
        headers.associate { header ->
            val nameValue = header.substringBefore(';')
            nameValue.substringBefore('=') to nameValue.substringAfter('=')
        }

    private companion object {
        private const val ACCESS_COOKIE = "access_token"
        private const val REFRESH_COOKIE = "refresh_token"
        private const val FUTURE_TIMEOUT_SECONDS = 30L
    }
}
