package ru.digitalhustle.certis.features.account.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.account.application.service.AccountApplicationService
import ru.digitalhustle.certis.features.account.command.model.NewAccount
import ru.digitalhustle.certis.features.account.command.model.UpdateAccountData
import ru.digitalhustle.certis.features.account.command.service.AccountService
import ru.digitalhustle.certis.features.account.model.AccountPreview
import ru.digitalhustle.certis.features.account.query.service.AccountQueryService
import ru.digitalhustle.certis.features.account.validator.AccountClosureValidator
import ru.digitalhustle.certis.features.transaction.api.RecurringTransactionUsage
import java.util.UUID

@Service
class AccountApplicationServiceImpl(
    private val accountQueryService: AccountQueryService,
    private val accountService: AccountService,
    private val recurringTransactionUsage: RecurringTransactionUsage,
    private val accountClosureValidator: AccountClosureValidator,
) : AccountApplicationService {

    override fun save(account: NewAccount): AccountPreview = accountService.save(account)

    @Transactional
    override fun update(account: UpdateAccountData): AccountPreview {
        accountService.update(account)
        return accountQueryService.getById(account.id, account.userId)
    }

    @Transactional
    override fun close(
        id: UUID,
        userId: UUID,
    ) {
        val account = accountService.getByIdForUpdate(id, userId)

        if (account.closedAt != null) {
            return
        }
        accountClosureValidator.validate(
            recurringTransactionUsage.existsSchedulableByAccountId(id, userId),
        )

        accountService.close(id, userId)
    }
}
