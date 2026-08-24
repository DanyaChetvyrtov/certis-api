package ru.digitalhustle.certis.config

import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import ru.digitalhustle.certis.fixture.UserFixture
import ru.digitalhustle.certis.gateway.MinioGateway
import ru.digitalhustle.certis.provider.SecurityRequestProvider
import ru.digitalhustle.certis.repository.AccountRepository
import ru.digitalhustle.certis.repository.BudgetRepository
import ru.digitalhustle.certis.repository.CategoryRepository
import ru.digitalhustle.certis.repository.ProfilePhotoMetaRepository
import ru.digitalhustle.certis.repository.ProfileRepository
import ru.digitalhustle.certis.repository.RecurringTransactionTemplateRepository
import ru.digitalhustle.certis.repository.RefreshSessionRepository
import ru.digitalhustle.certis.repository.TransactionRepository
import ru.digitalhustle.certis.repository.TransferRepository
import ru.digitalhustle.certis.repository.UserRepository
import ru.digitalhustle.certis.service.security.JwtTokenProvider
import ru.digitalhustle.certis.service.transaction.impl.RecurringTransactionExecutionServiceImpl

@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(
    provider = ZONKY,
    type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
    refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD,
)
@ActiveProfiles("test")
@SpringBootTest
abstract class AbstractIntegrationTest : AbstractResultActionsHelper() {

    @Autowired
    protected lateinit var dsl: DSLContext

    @Autowired
    protected lateinit var accountRepository: AccountRepository

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var refreshSessionRepository: RefreshSessionRepository

    @Autowired
    protected lateinit var transactionRepository: TransactionRepository

    @Autowired
    protected lateinit var recurringTransactionTemplateRepository: RecurringTransactionTemplateRepository

    @Autowired
    protected lateinit var transferRepository: TransferRepository

    @Autowired
    protected lateinit var categoryRepository: CategoryRepository

    @Autowired
    protected lateinit var budgetRepository: BudgetRepository

    @Autowired
    protected lateinit var profileRepository: ProfileRepository

    @Autowired
    protected lateinit var profilePhotoMetaRepository: ProfilePhotoMetaRepository

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    protected lateinit var jwtTokenProvider: JwtTokenProvider

    @Autowired
    protected lateinit var recurringTransactionExecutionService: RecurringTransactionExecutionServiceImpl

    @Autowired
    protected lateinit var userFixture: UserFixture

    @Autowired
    protected lateinit var securityRequestProvider: SecurityRequestProvider

    @Autowired
    protected lateinit var mvc: MockMvc

    @MockitoBean
    protected lateinit var minioGateway: MinioGateway
}
