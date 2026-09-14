package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.AccountController
import ru.digitalhustle.certis.api.dto.AccountDto
import ru.digitalhustle.certis.api.dto.request.CreateAccountRq
import ru.digitalhustle.certis.api.dto.request.UpdateAccountRq
import ru.digitalhustle.certis.api.dto.response.AccountsRs
import ru.digitalhustle.certis.api.mapper.AccountMapper
import ru.digitalhustle.certis.features.account.application.service.AccountApplicationService
import ru.digitalhustle.certis.features.account.query.service.AccountQueryService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@RestController
class AccountControllerImpl(
    private val accountQueryService: AccountQueryService,
    private val accountApplicationService: AccountApplicationService,
    private val accountMapper: AccountMapper,
) : AccountController {

    override fun getAccounts(jwtDetails: JwtDetails): AccountsRs =
        AccountsRs(
            accountQueryService.getAllByUserId(jwtDetails.id)
                .map(accountMapper::convert),
        )

    override fun getAccountById(
        accountId: UUID,
        jwtDetails: JwtDetails,
    ): AccountDto =
        accountMapper.convert(
            accountQueryService.getById(accountId, jwtDetails.id),
        )

    override fun createAccount(
        createAccountRq: CreateAccountRq,
        jwtDetails: JwtDetails,
    ): AccountDto =
        accountMapper.convert(
            accountApplicationService.save(
                accountMapper.convert(createAccountRq, jwtDetails.id),
            ),
        )

    override fun updateAccount(
        accountId: UUID,
        updateAccountRq: UpdateAccountRq,
        jwtDetails: JwtDetails,
    ): AccountDto =
        accountMapper.convert(
            accountApplicationService.update(
                accountMapper.convert(updateAccountRq, accountId, jwtDetails.id),
            ),
        )

    override fun closeAccount(
        accountId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = accountApplicationService.close(accountId, jwtDetails.id)
}
