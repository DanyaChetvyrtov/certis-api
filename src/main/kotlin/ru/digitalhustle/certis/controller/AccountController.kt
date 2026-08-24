package ru.digitalhustle.certis.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.AccountDto
import ru.digitalhustle.certis.dto.request.CreateAccountRq
import ru.digitalhustle.certis.dto.request.UpdateAccountRq
import ru.digitalhustle.certis.model.security.JwtDetails
import java.util.UUID

@RequestMapping(PathConstants.ACCOUNTS)
interface AccountController {

    @GetMapping
    fun getAccounts(
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): List<AccountDto>

    @GetMapping(PathConstants.ACCOUNT_ID)
    fun getAccountById(
        @PathVariable accountId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): AccountDto

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAccount(
        @RequestBody @Valid createAccountRq: CreateAccountRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): AccountDto

    @PutMapping(PathConstants.ACCOUNT_ID)
    fun updateAccount(
        @PathVariable accountId: UUID,
        @RequestBody @Valid updateAccountRq: UpdateAccountRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): AccountDto

    @DeleteMapping(PathConstants.ACCOUNT_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun closeAccount(
        @PathVariable accountId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    )
}
