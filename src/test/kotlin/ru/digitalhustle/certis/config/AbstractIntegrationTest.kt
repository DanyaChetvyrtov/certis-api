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
import ru.digitalhustle.certis.features.account.command.repository.AccountRepository
import ru.digitalhustle.certis.features.account.query.repository.AccountQueryRepository
import ru.digitalhustle.certis.features.budget.command.repository.BudgetAllocationRepository
import ru.digitalhustle.certis.features.budget.command.repository.BudgetRepository
import ru.digitalhustle.certis.features.budget.query.repository.BudgetQueryRepository
import ru.digitalhustle.certis.features.category.command.repository.CategoryRepository
import ru.digitalhustle.certis.features.category.query.repository.CategoryQueryRepository
import ru.digitalhustle.certis.features.goal.command.repository.GoalRepository
import ru.digitalhustle.certis.features.goal.command.repository.GoalTransactionRepository
import ru.digitalhustle.certis.features.goal.query.repository.GoalTransactionQueryRepository
import ru.digitalhustle.certis.features.profile.command.repository.ProfilePhotoMetaRepository
import ru.digitalhustle.certis.features.profile.command.repository.ProfileRepository
import ru.digitalhustle.certis.features.profile.gateway.MinioGateway
import ru.digitalhustle.certis.features.profile.query.repository.ProfilePhotoMetaQueryRepository
import ru.digitalhustle.certis.features.profile.query.repository.ProfileQueryRepository
import ru.digitalhustle.certis.features.security.command.repository.RefreshSessionRepository
import ru.digitalhustle.certis.features.security.command.repository.UserRepository
import ru.digitalhustle.certis.features.security.infrastructure.service.JwtTokenProvider
import ru.digitalhustle.certis.features.security.query.repository.RefreshSessionQueryRepository
import ru.digitalhustle.certis.features.security.query.repository.UserQueryRepository
import ru.digitalhustle.certis.features.transaction.command.repository.RecurringTransactionTemplateRepository
import ru.digitalhustle.certis.features.transaction.command.repository.TransactionRepository
import ru.digitalhustle.certis.features.transaction.command.repository.TransferRepository
import ru.digitalhustle.certis.features.transaction.command.service.impl.RecurringTransactionExecutionServiceImpl
import ru.digitalhustle.certis.features.transaction.query.repository.RecurringTransactionTemplateQueryRepository
import ru.digitalhustle.certis.features.transaction.query.repository.TransactionQueryRepository
import ru.digitalhustle.certis.features.transaction.query.repository.TransferQueryRepository
import ru.digitalhustle.certis.fixture.UserFixture
import ru.digitalhustle.certis.provider.SecurityRequestProvider

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
    protected lateinit var accountQueryRepository: AccountQueryRepository

    @Autowired
    protected lateinit var categoryQueryRepository: CategoryQueryRepository

    @Autowired
    protected lateinit var budgetQueryRepository: BudgetQueryRepository

    @Autowired
    protected lateinit var userQueryRepository: UserQueryRepository

    @Autowired
    protected lateinit var refreshSessionQueryRepository: RefreshSessionQueryRepository

    @Autowired
    protected lateinit var profileQueryRepository: ProfileQueryRepository

    @Autowired
    protected lateinit var profilePhotoMetaQueryRepository: ProfilePhotoMetaQueryRepository

    @Autowired
    protected lateinit var goalTransactionQueryRepository: GoalTransactionQueryRepository

    @Autowired
    protected lateinit var budgetAllocationRepository: BudgetAllocationRepository

    @Autowired
    protected lateinit var dsl: DSLContext

    @Autowired
    protected lateinit var accountRepository: AccountRepository

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var refreshSessionRepository: RefreshSessionRepository

    @Autowired
    protected lateinit var transactionQueryRepository: TransactionQueryRepository

    @Autowired
    protected lateinit var transactionRepository: TransactionRepository

    @Autowired
    protected lateinit var recurringTransactionTemplateQueryRepository: RecurringTransactionTemplateQueryRepository

    @Autowired
    protected lateinit var recurringTransactionTemplateRepository: RecurringTransactionTemplateRepository

    @Autowired
    protected lateinit var transferQueryRepository: TransferQueryRepository

    @Autowired
    protected lateinit var transferRepository: TransferRepository

    @Autowired
    protected lateinit var categoryRepository: CategoryRepository

    @Autowired
    protected lateinit var goalRepository: GoalRepository

    @Autowired
    protected lateinit var goalTransactionRepository: GoalTransactionRepository

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
