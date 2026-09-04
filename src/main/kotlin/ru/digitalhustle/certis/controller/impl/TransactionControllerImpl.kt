package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.TransactionController
import ru.digitalhustle.certis.dto.TransactionDto
import ru.digitalhustle.certis.dto.request.AssignTransactionsCategoryRq
import ru.digitalhustle.certis.dto.request.CreateTransactionRq
import ru.digitalhustle.certis.dto.request.TransactionFilterRq
import ru.digitalhustle.certis.dto.request.UncategorizedTransactionFilterRq
import ru.digitalhustle.certis.dto.request.UpdateTransactionRq
import ru.digitalhustle.certis.dto.response.TransactionPageRs
import ru.digitalhustle.certis.dto.response.UncategorizedTransactionPageRs
import ru.digitalhustle.certis.mapper.TransactionCategorizationMapper
import ru.digitalhustle.certis.mapper.TransactionMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.transaction.TransactionAggregator
import java.util.UUID

@RestController
class TransactionControllerImpl(
    private val transactionAggregator: TransactionAggregator,
    private val transactionMapper: TransactionMapper,
    private val transactionCategorizationMapper: TransactionCategorizationMapper,
) : TransactionController {

    override fun getTransactions(
        filterRq: TransactionFilterRq,
        jwtDetails: JwtDetails,
    ): TransactionPageRs =
        transactionMapper.convert(
            transactionAggregator.getAllByUserId(
                userId = jwtDetails.id,
                filter = transactionMapper.convert(filterRq),
            ),
        )

    override fun getUncategorizedTransactions(
        filterRq: UncategorizedTransactionFilterRq,
        jwtDetails: JwtDetails,
    ): UncategorizedTransactionPageRs =
        transactionCategorizationMapper.convert(
            transactionAggregator.getUncategorizedByUserId(
                userId = jwtDetails.id,
                filter = transactionCategorizationMapper.convert(filterRq),
            ),
        )

    override fun getTransactionById(
        transactionId: UUID,
        jwtDetails: JwtDetails,
    ): TransactionDto =
        transactionMapper.convert(
            transactionAggregator.getById(transactionId, jwtDetails.id),
        )

    override fun createTransaction(
        createTransactionRq: CreateTransactionRq,
        jwtDetails: JwtDetails,
    ): TransactionDto =
        transactionMapper.convert(
            transactionAggregator.save(
                transactionMapper.convert(createTransactionRq, jwtDetails.id),
            ),
        )

    override fun updateTransaction(
        transactionId: UUID,
        updateTransactionRq: UpdateTransactionRq,
        jwtDetails: JwtDetails,
    ): TransactionDto =
        transactionMapper.convert(
            transactionAggregator.update(
                transactionMapper.convert(updateTransactionRq, transactionId, jwtDetails.id),
            ),
        )

    override fun assignTransactionsCategory(
        assignCategoryRq: AssignTransactionsCategoryRq,
        jwtDetails: JwtDetails,
    ): Unit =
        transactionAggregator.assignCategories(
            transactionCategorizationMapper.convert(assignCategoryRq, jwtDetails.id),
        )

    override fun deleteTransaction(
        transactionId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = transactionAggregator.delete(transactionId, jwtDetails.id)
}
