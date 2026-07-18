package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.request.LoginRq
import ru.digitalhustle.certis.dto.request.RegisterRq

class AuthControllerIT : AbstractIntegrationTest() {

    @Test
    fun `should register user`() {
        // given
        val request = RegisterRq(
            email = "john@test.com",
            password = "password",
            passwordConfirmation = "password",
        )

        // when
        mvc.perform(
            post(PathConstants.AUTH + PathConstants.REGISTRATION)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request))
        )

            // then
            .andExpect(status().isCreated)

        val user = requireNotNull(
            userRepository.findByEmail("john@test.com")
        )

        assertThat(user.email)
            .isEqualTo("john@test.com")
    }

    @Test
    fun `should return 409 when email already exists`() {
        // given
        userFixture.create {
            copy(email = "john@test.com")
        }

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
    fun `should return 400 when password confirmation doesn't match`() {
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

        val request = LoginRq(
            email = "user@test.com",
            password = "password",
        )

        // when
        val result = mvc.perform(
            post(PathConstants.AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )

        // then
        result
            .andExpect(status().isOk)
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))

        val cookies = result
            .andReturn()
            .response
            .getHeaders(HttpHeaders.SET_COOKIE)

        assertThat(cookies)
            .anyMatch { it.startsWith("access_token=") }

        assertThat(cookies)
            .anyMatch { it.startsWith("refresh_token=") }
    }

    @Test
    fun `should return 401 when password is invalid`() {
        // given
        userFixture.create()

        val request = LoginRq(
            email = "user@test.com",
            password = "wrong_password",
        )

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
    }

    @Test
    fun `should return 404 when user doesn't exist`() {
        // given
        val request = LoginRq(
            email = "unknown@test.com",
            password = "password",
        )

        // when
        mvc.perform(
            post(PathConstants.AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)),
        )

            // then
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("User not found"))
    }

    @Test
    fun `should refresh access token`() {
        // given
        val user = userFixture.create()

        val refreshToken = jwtTokenProvider.createRefreshToken(
            user.id,
            user.email,
        )

        // when
        val result = mvc.perform(
            post(PathConstants.AUTH + PathConstants.TOKENS_ACCESS)
                .cookie(
                    Cookie(
                        "refresh_token",
                        refreshToken,
                    ),
                ),
        )

        // then
        result
            .andExpect(status().isNoContent)
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))
    }

    @Test
    fun `should refresh both tokens`() {
        // given
        val user = userFixture.create()

        val refreshToken = jwtTokenProvider.createRefreshToken(
            user.id,
            user.email,
        )

        // when
        val result = mvc.perform(
            post(PathConstants.AUTH + PathConstants.TOKENS_BOTH)
                .cookie(
                    Cookie(
                        "refresh_token",
                        refreshToken,
                    ),
                ),
        )

        // then
        result
            .andExpect(status().isNoContent)
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))

        val cookies = result
            .andReturn()
            .response
            .getHeaders(HttpHeaders.SET_COOKIE)

        assertThat(cookies)
            .anyMatch { it.startsWith("access_token=") }

        assertThat(cookies)
            .anyMatch { it.startsWith("refresh_token=") }
    }

    @Test
    fun `should return 401 when refresh token is invalid`() {
        // when
        mvc.perform(
            post(PathConstants.AUTH + PathConstants.TOKENS_ACCESS)
                .cookie(
                    Cookie(
                        "refresh_token",
                        "invalid_refresh_token",
                    ),
                ),
        )

            // then
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"))
    }
}
