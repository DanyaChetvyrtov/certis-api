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
import ru.digitalhustle.certis.dto.RecurringTransactionDto
import ru.digitalhustle.certis.dto.request.CreateRecurringTransactionRq
import ru.digitalhustle.certis.dto.request.UpdateRecurringTransactionRq
import ru.digitalhustle.certis.model.security.JwtDetails
import java.util.UUID

@RequestMapping(PathConstants.RECURRING_TRANSACTIONS)
interface RecurringTransactionController {

    @GetMapping
    fun getRecurringTransactions(
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): List<RecurringTransactionDto>

    @GetMapping(PathConstants.RECURRING_TRANSACTION_ID)
    fun getRecurringTransactionById(
        @PathVariable recurringTransactionId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): RecurringTransactionDto

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createRecurringTransaction(
        @RequestBody @Valid createRecurringTransactionRq: CreateRecurringTransactionRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): RecurringTransactionDto

    @PutMapping(PathConstants.RECURRING_TRANSACTION_ID)
    fun updateRecurringTransaction(
        @PathVariable recurringTransactionId: UUID,
        @RequestBody @Valid updateRecurringTransactionRq: UpdateRecurringTransactionRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): RecurringTransactionDto

    @DeleteMapping(PathConstants.RECURRING_TRANSACTION_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancelRecurringTransaction(
        @PathVariable recurringTransactionId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    )
}
