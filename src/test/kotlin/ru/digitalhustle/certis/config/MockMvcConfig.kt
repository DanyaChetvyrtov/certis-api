package ru.digitalhustle.certis.config

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import ru.digitalhustle.certis.constants.PathConstants

@TestConfiguration
class MockMvcConfig {

    companion object {
        fun basicAuth(username: String?, password: String?): RequestPostProcessor = RequestPostProcessor { request ->
            request.removeHeader(HttpHeaders.AUTHORIZATION)
            httpBasic(username, password).postProcessRequest(request)
        }
    }

    @Bean
    fun httpBasicHeader(): MockMvcBuilderCustomizer? = MockMvcBuilderCustomizer { builder ->
        builder.defaultRequest(
            post(PathConstants.API_V1),
        )
    }
}
