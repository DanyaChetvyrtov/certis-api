package ru.digitalhustle.certis.integrations

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhustle.certis.config.AbstractIntegrationTest
import ru.digitalhustle.certis.constants.SecurityConstants

class SecurityEndpointsIT : AbstractIntegrationTest() {

    @Test
    fun `should expose health without details`() {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist())
    }

    @Test
    fun `should not expose other actuator endpoints`() {
        // given
        val accessCookie = accessCookie()

        // when, then
        mvc.perform(get("/actuator/env").cookie(accessCookie))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should keep swagger disabled by default`() {
        // given
        val accessCookie = accessCookie()

        // when, then
        mvc.perform(get("/v3/api-docs").cookie(accessCookie))
            .andExpect(status().isNotFound)
    }

    private fun accessCookie(): Cookie {
        val user = userFixture.create()
        val token = jwtTokenProvider.createAccessToken(user.id, user.email)

        return Cookie(SecurityConstants.ACCESS_TOKEN_COOKIE, token)
    }
}
