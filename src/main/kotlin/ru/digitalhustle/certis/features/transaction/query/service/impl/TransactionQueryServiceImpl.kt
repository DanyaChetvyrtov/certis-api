package ru.digitalhustle.certis.features.transaction.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.transaction.model.Transaction
import ru.digitalhustle.certis.features.transaction.query.model.TransactionFilter
import ru.digitalhustle.certis.features.transaction.query.model.TransactionPage
import ru.digitalhustle.certis.features.transaction.query.repository.TransactionQueryRepository
import ru.digitalhustle.certis.features.transaction.query.service.TransactionQueryService
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TransactionQueryServiceImpl(
    private val transactionRepository: TransactionQueryRepository,
) : TransactionQueryService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): Transaction =
        transactionRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.entity("Transaction")

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    override fun getAllByUserId(
        userId: UUID,
        filter: TransactionFilter,
    ): TransactionPage = transactionRepository.findAllByUserId(userId, filter)
}
