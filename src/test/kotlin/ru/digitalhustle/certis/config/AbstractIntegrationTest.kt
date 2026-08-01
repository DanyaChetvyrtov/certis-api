package ru.digitalhustle.certis.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import ru.digitalhustle.certis.fixture.UserFixture
import ru.digitalhustle.certis.gateway.MinioGateway
import ru.digitalhustle.certis.repository.ProfilePhotoMetaRepository
import ru.digitalhustle.certis.repository.ProfileRepository
import ru.digitalhustle.certis.repository.RefreshSessionRepository
import ru.digitalhustle.certis.repository.UserRepository
import ru.digitalhustle.certis.service.security.JwtTokenProvider

@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(
    provider = ZONKY,
    type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
    refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD,
)
@ActiveProfiles("test")
@SpringBootTest
abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var refreshSessionRepository: RefreshSessionRepository

    @Autowired
    protected lateinit var profileRepository: ProfileRepository

    @Autowired
    protected lateinit var profilePhotoMetaRepository: ProfilePhotoMetaRepository

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    protected lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    protected lateinit var userFixture: UserFixture

    @Autowired
    protected lateinit var mvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @MockitoBean
    protected lateinit var minioGateway: MinioGateway
}
