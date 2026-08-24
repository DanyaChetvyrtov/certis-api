package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.constants.ErrorMessages
import ru.digitalhustle.certis.exception.custom.AccountClosedException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.mapper.toPreview
import ru.digitalhustle.certis.model.account.AccountBalanceDelta
import ru.digitalhustle.certis.model.account.AccountPreview
import ru.digitalhustle.certis.model.account.NewAccount
import ru.digitalhustle.certis.model.account.UpdateAccountData
import ru.digitalhustle.certis.model.entity.Account
import ru.digitalhustle.certis.repository.AccountBalanceRepository
import ru.digitalhustle.certis.repository.AccountRepository
import ru.digitalhustle.certis.service.domain.AccountService
import java.math.BigDecimal
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AccountServiceImpl(
    private val accountRepository: AccountRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val clock: Clock,
) : AccountService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): AccountPreview {
        val account = getAccount(id, userId)

        return addBalances(listOf(account), userId).single()
    }

    override fun getByIdForShare(
        id: UUID,
        userId: UUID,
    ): Account =
        accountRepository.findByIdAndUserIdForShare(id, userId)
            ?: throw NotFoundException.entity("Account")

    override fun getByIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Account =
        accountRepository.findByIdAndUserIdForUpdate(id, userId)
            ?: throw NotFoundException.entity("Account")

    override fun getAllByIdsForShare(
        ids: Collection<UUID>,
        userId: UUID,
    ): List<Account> {
        val requestedIds = ids.toSet()
        val accounts = accountRepository.findAllByIdsAndUserIdForShare(requestedIds, userId)

        if (accounts.size != requestedIds.size) {
            throw NotFoundException.entity("Account")
        }

        return accounts
    }

    override fun getAllByUserId(userId: UUID): List<AccountPreview> =
        addBalances(accountRepository.findAllByUserId(userId), userId)

    override fun save(newAccount: NewAccount): AccountPreview {
        val account = accountRepository.insert(
            Account(
                id = UUID.randomUUID(),
                userId = newAccount.userId,
                name = newAccount.name,
                type = newAccount.type,
                openingBalance = newAccount.openingBalance,
                currency = newAccount.currency,
                createdAt = OffsetDateTime.now(clock),
                closedAt = null,
            ),
        )

        return account.toPreview(account.openingBalance)
    }

    override fun update(account: UpdateAccountData): AccountPreview {
        val updatedAccount = accountRepository.updateActive(account)
            ?: throwUpdateFailure(account.id, account.userId)

        return addBalances(listOf(updatedAccount), account.userId).single()
    }

    override fun close(
        id: UUID,
        userId: UUID,
    ) {
        val closed = accountRepository.close(
            id = id,
            userId = userId,
            closedAt = OffsetDateTime.now(clock),
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

        throw NotFoundException.entity("Account")
    }

    private fun getAccount(id: UUID, userId: UUID): Account =
        accountRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.entity("Account")

    private fun addBalances(
        accounts: List<Account>,
        userId: UUID,
    ): List<AccountPreview> {
        if (accounts.isEmpty()) {
            return emptyList()
        }

        val balanceDeltas = accountBalanceRepository.findBalanceDeltas(
            userId = userId,
            accountIds = accounts.map(Account::id),
        ).associateBy(AccountBalanceDelta::accountId)

        return accounts.map { account ->
            account.toPreview(
                account.openingBalance + (balanceDeltas[account.id]?.delta ?: BigDecimal.ZERO),
            )
        }
    }
}
