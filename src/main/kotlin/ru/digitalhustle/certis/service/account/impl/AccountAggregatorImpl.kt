package ru.digitalhustle.certis.service.account.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.AccountInUseException
import ru.digitalhustle.certis.model.account.AccountPreview
import ru.digitalhustle.certis.model.account.NewAccount
import ru.digitalhustle.certis.model.account.UpdateAccountData
import ru.digitalhustle.certis.service.account.AccountAggregator
import ru.digitalhustle.certis.service.domain.AccountService
import ru.digitalhustle.certis.service.domain.RecurringTransactionTemplateService
import java.util.UUID

@Service
class AccountAggregatorImpl(
    private val accountService: AccountService,
    private val recurringTransactionTemplateService: RecurringTransactionTemplateService,
) : AccountAggregator {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): AccountPreview = accountService.getById(id, userId)

    override fun getAllByUserId(userId: UUID): List<AccountPreview> =
        accountService.getAllByUserId(userId)

    override fun save(account: NewAccount): AccountPreview = accountService.save(account)

    override fun update(account: UpdateAccountData): AccountPreview = accountService.update(account)

    @Transactional
    override fun close(
        id: UUID,
        userId: UUID,
    ) {
        val account = accountService.getByIdForUpdate(id, userId)

        if (account.closedAt != null) {
            return
        }
        if (recurringTransactionTemplateService.existsSchedulableByAccountId(id, userId)) {
            throw AccountInUseException(ErrorMessages.ACCOUNT_IN_USE)
        }

        accountService.close(id, userId)
    }
}
