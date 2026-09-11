package ru.digitalhustle.certis.features.transaction.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.transaction.model.Transfer
import ru.digitalhustle.certis.features.transaction.query.repository.TransferQueryRepository
import ru.digitalhustle.certis.features.transaction.query.service.TransferQueryService
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TransferQueryServiceImpl(
    private val transferRepository: TransferQueryRepository,
) : TransferQueryService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): Transfer =
        transferRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.entity("Transfer")

    override fun getAllByUserId(userId: UUID): List<Transfer> =
        transferRepository.findAllByUserId(userId)
}
