package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.TransactionController
import ru.digitalhustle.certis.api.dto.TransactionDto
import ru.digitalhustle.certis.api.dto.request.AssignTransactionsCategoryRq
import ru.digitalhustle.certis.api.dto.request.CashFlowAnalyticsRq
import ru.digitalhustle.certis.api.dto.request.CreateTransactionRq
import ru.digitalhustle.certis.api.dto.request.MonthlyTransactionAnalyticsRq
import ru.digitalhustle.certis.api.dto.request.TransactionFilterRq
import ru.digitalhustle.certis.api.dto.request.UncategorizedTransactionFilterRq
import ru.digitalhustle.certis.api.dto.request.UpdateTransactionRq
import ru.digitalhustle.certis.api.dto.response.CashFlowAnalyticsRs
import ru.digitalhustle.certis.api.dto.response.MonthlyTransactionAnalyticsRs
import ru.digitalhustle.certis.api.dto.response.TransactionPageRs
import ru.digitalhustle.certis.api.dto.response.UncategorizedTransactionPageRs
import ru.digitalhustle.certis.api.mapper.TransactionAnalyticsMapper
import ru.digitalhustle.certis.api.mapper.TransactionCategorizationMapper
import ru.digitalhustle.certis.api.mapper.TransactionMapper
import ru.digitalhustle.certis.features.security.model.JwtDetails
import ru.digitalhustle.certis.features.transaction.command.service.TransactionCommandService
import ru.digitalhustle.certis.features.transaction.query.service.TransactionAnalyticsService
import ru.digitalhustle.certis.features.transaction.query.service.TransactionQueryService
import ru.digitalhustle.certis.features.transaction.query.service.UncategorizedTransactionService
import java.util.UUID

@RestController
class TransactionControllerImpl(
    private val uncategorizedTransactionService: UncategorizedTransactionService,
    private val transactionQueryService: TransactionQueryService,
    private val transactionCommandService: TransactionCommandService,
    private val transactionMapper: TransactionMapper,
    private val transactionAnalyticsMapper: TransactionAnalyticsMapper,
    private val transactionCategorizationMapper: TransactionCategorizationMapper,
    private val transactionAnalyticsService: TransactionAnalyticsService,
) : TransactionController {

    override fun getTransactions(
        filterRq: TransactionFilterRq,
        jwtDetails: JwtDetails,
    ): TransactionPageRs =
        transactionMapper.convert(
            transactionQueryService.getAllByUserId(
                userId = jwtDetails.id,
                filter = transactionMapper.convert(filterRq),
            ),
        )

    override fun getMonthlyAnalytics(
        analyticsRq: MonthlyTransactionAnalyticsRq,
        jwtDetails: JwtDetails,
    ): MonthlyTransactionAnalyticsRs =
        transactionAnalyticsMapper.convert(
            transactionAnalyticsService.getMonthlyAnalytics(
                userId = jwtDetails.id,
                filter = transactionAnalyticsMapper.convert(analyticsRq),
            ),
        )

    override fun getCashFlowAnalytics(
        analyticsRq: CashFlowAnalyticsRq,
        jwtDetails: JwtDetails,
    ): CashFlowAnalyticsRs =
        transactionAnalyticsMapper.convert(
            transactionAnalyticsService.getCashFlowAnalytics(
                userId = jwtDetails.id,
                filter = transactionAnalyticsMapper.convert(analyticsRq),
            ),
        )

    override fun getUncategorizedTransactions(
        filterRq: UncategorizedTransactionFilterRq,
        jwtDetails: JwtDetails,
    ): UncategorizedTransactionPageRs =
        transactionCategorizationMapper.convert(
            uncategorizedTransactionService.getAllByUserId(
                userId = jwtDetails.id,
                filter = transactionCategorizationMapper.convert(filterRq),
            ),
        )

    override fun getTransactionById(
        transactionId: UUID,
        jwtDetails: JwtDetails,
    ): TransactionDto =
        transactionMapper.convert(
            transactionQueryService.getById(transactionId, jwtDetails.id),
        )

    override fun createTransaction(
        createTransactionRq: CreateTransactionRq,
        jwtDetails: JwtDetails,
    ): TransactionDto =
        transactionMapper.convert(
            transactionCommandService.save(
                transactionMapper.convert(createTransactionRq, jwtDetails.id),
            ),
        )

    override fun updateTransaction(
        transactionId: UUID,
        updateTransactionRq: UpdateTransactionRq,
        jwtDetails: JwtDetails,
    ): TransactionDto =
        transactionMapper.convert(
            transactionCommandService.update(
                transactionMapper.convert(updateTransactionRq, transactionId, jwtDetails.id),
            ),
        )

    override fun assignTransactionsCategory(
        assignCategoryRq: AssignTransactionsCategoryRq,
        jwtDetails: JwtDetails,
    ): Unit =
        transactionCommandService.assignCategories(
            transactionCategorizationMapper.convert(assignCategoryRq, jwtDetails.id),
        )

    override fun deleteTransaction(
        transactionId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = transactionCommandService.delete(transactionId, jwtDetails.id)
}
