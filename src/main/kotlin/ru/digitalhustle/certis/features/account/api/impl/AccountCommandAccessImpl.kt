package ru.digitalhustle.certis.features.account.api.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.account.api.AccountCommandAccess
import ru.digitalhustle.certis.features.account.api.AccountSnapshot
import ru.digitalhustle.certis.features.account.command.service.AccountService
import ru.digitalhustle.certis.features.account.model.Account
import java.util.UUID

@Service
@Transactional(propagation = Propagation.MANDATORY)
class AccountCommandAccessImpl(
    private val accountService: AccountService,
) : AccountCommandAccess {

    override fun getByIdForShare(id: UUID, userId: UUID): AccountSnapshot =
        accountService.getByIdForShare(id, userId).toSnapshot()

    override fun getAllByIdsForShare(ids: Collection<UUID>, userId: UUID): List<AccountSnapshot> =
        accountService.getAllByIdsForShare(ids, userId).map { it.toSnapshot() }

    private fun Account.toSnapshot(): AccountSnapshot =
        AccountSnapshot(
            id = id,
            currency = currency,
            closedAt = closedAt,
        )
}
