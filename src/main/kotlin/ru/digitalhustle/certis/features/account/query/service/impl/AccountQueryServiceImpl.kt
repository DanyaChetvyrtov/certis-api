package ru.digitalhustle.certis.features.account.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.account.model.Account
import ru.digitalhustle.certis.features.account.model.AccountPreview
import ru.digitalhustle.certis.features.account.model.toPreview
import ru.digitalhustle.certis.features.account.query.model.AccountBalanceDelta
import ru.digitalhustle.certis.features.account.query.repository.AccountQueryRepository
import ru.digitalhustle.certis.features.account.query.service.AccountQueryService
import java.math.BigDecimal
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AccountQueryServiceImpl(
    private val accountRepository: AccountQueryRepository,
) : AccountQueryService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): AccountPreview {
        val account = getAccount(id, userId)

        return addBalances(listOf(account), userId).single()
    }

    override fun getAllByUserId(userId: UUID): List<AccountPreview> =
        addBalances(accountRepository.findAllByUserId(userId), userId)

    private fun getAccount(id: UUID, userId: UUID): Account =
        accountRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.Companion.entity("Account")

    private fun addBalances(
        accounts: List<Account>,
        userId: UUID,
    ): List<AccountPreview> {
        if (accounts.isEmpty()) {
            return emptyList()
        }

        val balanceDeltas = accountRepository.findBalanceDeltas(
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
