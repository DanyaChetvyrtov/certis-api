package ru.digitalhustle.certis.features.account.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.account.command.model.NewAccount
import ru.digitalhustle.certis.features.account.command.model.UpdateAccountData
import ru.digitalhustle.certis.features.account.command.repository.AccountRepository
import ru.digitalhustle.certis.features.account.command.service.AccountService
import ru.digitalhustle.certis.features.account.exceptions.AccountClosedException
import ru.digitalhustle.certis.features.account.model.Account
import ru.digitalhustle.certis.features.account.model.AccountPreview
import ru.digitalhustle.certis.features.account.model.toPreview
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
class AccountServiceImpl(
    private val accountRepository: AccountRepository,
    private val applicationClock: ApplicationClock,
) : AccountService {

    override fun getByIdForShare(
        id: UUID,
        userId: UUID,
    ): Account =
        accountRepository.findByIdAndUserIdForShare(id, userId)
            ?: throw NotFoundException.Companion.entity("Account")

    override fun getByIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Account =
        accountRepository.findByIdAndUserIdForUpdate(id, userId)
            ?: throw NotFoundException.Companion.entity("Account")

    override fun getAllByIdsForShare(
        ids: Collection<UUID>,
        userId: UUID,
    ): List<Account> {
        val requestedIds = ids.toSet()
        val accounts = accountRepository.findAllByIdsAndUserIdForShare(requestedIds, userId)

        if (accounts.size != requestedIds.size) {
            throw NotFoundException.Companion.entity("Account")
        }

        return accounts
    }

    override fun save(newAccount: NewAccount): AccountPreview {
        val account = accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = newAccount.userId,
                name = newAccount.name,
                type = newAccount.type,
                openingBalance = newAccount.openingBalance,
                currency = newAccount.currency,
                createdAt = applicationClock.now(),
                closedAt = null,
            ),
        )

        return account.toPreview(account.openingBalance)
    }

    override fun update(account: UpdateAccountData): Account {
        val updatedAccount = accountRepository.updateActive(account)
            ?: throwUpdateFailure(account.id, account.userId)

        return updatedAccount
    }

    override fun close(
        id: UUID,
        userId: UUID,
    ) {
        val closed = accountRepository.close(
            id = id,
            userId = userId,
            closedAt = applicationClock.now(),
        )

        if (!closed) {
            getAccount(id, userId)
        }
    }

    private fun throwUpdateFailure(
        id: UUID,
        userId: UUID,
    ): Nothing {
        val account = getAccount(id, userId)

        if (account.closedAt != null) {
            throw AccountClosedException(ErrorMessages.ACCOUNT_CLOSED)
        }

        throw NotFoundException.Companion.entity("Account")
    }

    private fun getAccount(id: UUID, userId: UUID): Account =
        accountRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.Companion.entity("Account")
}
