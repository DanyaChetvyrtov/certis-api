package ru.digitalhustle.certis.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(
    provider = ZONKY,
    type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
    refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD,
)
@ActiveProfiles("test", "dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [MockMvcConfig::class],
)
abstract class AbstractIntegrationTest(

    @Autowired
    protected var mvc: MockMvc,

    @Autowired
    override var objectMapper: ObjectMapper,

    ) : AbstractResultActionsHelper(objectMapper)
