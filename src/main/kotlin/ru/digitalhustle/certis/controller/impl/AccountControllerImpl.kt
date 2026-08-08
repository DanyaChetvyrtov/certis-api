package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.AccountController
import ru.digitalhustle.certis.dto.AccountDto
import ru.digitalhustle.certis.dto.request.CreateAccountRq
import ru.digitalhustle.certis.dto.request.UpdateAccountRq
import ru.digitalhustle.certis.mapper.AccountMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.domain.AccountService
import java.util.UUID

@RestController
class AccountControllerImpl(
    private val accountService: AccountService,
    private val accountMapper: AccountMapper,
) : AccountController {

    override fun getAccounts(jwtDetails: JwtDetails): List<AccountDto> =
        accountService.getAllByUserId(jwtDetails.id)
            .map(accountMapper::convert)

    override fun getAccountById(
        accountId: UUID,
        jwtDetails: JwtDetails,
    ): AccountDto =
        accountMapper.convert(
            accountService.getById(accountId, jwtDetails.id),
        )

    override fun createAccount(
        createAccountRq: CreateAccountRq,
        jwtDetails: JwtDetails,
    ): AccountDto =
        accountMapper.convert(
            accountService.save(
                accountMapper.convert(createAccountRq, jwtDetails.id),
            ),
        )

    override fun updateAccount(
        accountId: UUID,
        updateAccountRq: UpdateAccountRq,
        jwtDetails: JwtDetails,
    ): AccountDto =
        accountMapper.convert(
            accountService.update(
                accountMapper.convert(updateAccountRq, accountId, jwtDetails.id),
            ),
        )

    override fun closeAccount(
        accountId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = accountService.close(accountId, jwtDetails.id)
}
