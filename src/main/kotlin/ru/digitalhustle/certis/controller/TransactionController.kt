package ru.digitalhustle.certis.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.dto.TransactionDto
import ru.digitalhustle.certis.dto.request.AssignTransactionsCategoryRq
import ru.digitalhustle.certis.dto.request.CashFlowAnalyticsRq
import ru.digitalhustle.certis.dto.request.CreateTransactionRq
import ru.digitalhustle.certis.dto.request.MonthlyTransactionAnalyticsRq
import ru.digitalhustle.certis.dto.request.TransactionFilterRq
import ru.digitalhustle.certis.dto.request.UncategorizedTransactionFilterRq
import ru.digitalhustle.certis.dto.request.UpdateTransactionRq
import ru.digitalhustle.certis.dto.response.CashFlowAnalyticsRs
import ru.digitalhustle.certis.dto.response.MonthlyTransactionAnalyticsRs
import ru.digitalhustle.certis.dto.response.TransactionPageRs
import ru.digitalhustle.certis.dto.response.UncategorizedTransactionPageRs
import ru.digitalhustle.certis.model.security.JwtDetails
import java.util.UUID

@RequestMapping(PathConstants.TRANSACTIONS)
interface TransactionController {

    @GetMapping
    fun getTransactions(
        @ModelAttribute @Valid filterRq: TransactionFilterRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): TransactionPageRs

    @GetMapping(PathConstants.TRANSACTION_ANALYTICS_MONTHLY)
    fun getMonthlyAnalytics(
        @ModelAttribute @Valid analyticsRq: MonthlyTransactionAnalyticsRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): MonthlyTransactionAnalyticsRs

    @GetMapping(PathConstants.TRANSACTION_ANALYTICS_CASH_FLOW)
    fun getCashFlowAnalytics(
        @ModelAttribute @Valid analyticsRq: CashFlowAnalyticsRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): CashFlowAnalyticsRs

    @GetMapping(PathConstants.TRANSACTION_UNCATEGORIZED)
    fun getUncategorizedTransactions(
        @ModelAttribute @Valid filterRq: UncategorizedTransactionFilterRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): UncategorizedTransactionPageRs

    @GetMapping(PathConstants.TRANSACTION_ID)
    fun getTransactionById(
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): TransactionDto

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTransaction(
        @RequestBody @Valid createTransactionRq: CreateTransactionRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): TransactionDto

    @PutMapping(PathConstants.TRANSACTION_ID)
    fun updateTransaction(
        @PathVariable transactionId: UUID,
        @RequestBody @Valid updateTransactionRq: UpdateTransactionRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): TransactionDto

    @PatchMapping(PathConstants.TRANSACTION_CATEGORY_ASSIGNMENTS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun assignTransactionsCategory(
        @RequestBody @Valid assignCategoryRq: AssignTransactionsCategoryRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    )

    @DeleteMapping(PathConstants.TRANSACTION_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTransaction(
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    )
}
