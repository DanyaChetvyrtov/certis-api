package ru.digitalhustle.certis.integrations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.PathConstants
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
}