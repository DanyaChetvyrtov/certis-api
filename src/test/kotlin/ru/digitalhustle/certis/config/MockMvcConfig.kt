package ru.digitalhustle.certis.config

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import ru.digitalhustle.certis.constants.PathConstants

@TestConfiguration
class MockMvcConfig {

    @Bean
    fun httpBasicHeader(): MockMvcBuilderCustomizer =
        MockMvcBuilderCustomizer { builder ->
            builder.defaultRequest(
                post(PathConstants.API_V1),
            )
        }
}
