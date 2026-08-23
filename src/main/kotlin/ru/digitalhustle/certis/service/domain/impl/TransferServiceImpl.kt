package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.Transfer
import ru.digitalhustle.certis.model.transfer.NewTransfer
import ru.digitalhustle.certis.repository.TransferRepository
import ru.digitalhustle.certis.service.domain.TransferService
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TransferServiceImpl(
    private val transferRepository: TransferRepository,
    private val clock: Clock,
) : TransferService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): Transfer =
        transferRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException.entity("Transfer")

    override fun getByIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Transfer =
        transferRepository.findByIdAndUserIdForUpdate(id, userId)
            ?: throw NotFoundException.entity("Transfer")

    override fun findReversal(
        transferId: UUID,
        userId: UUID,
    ): Transfer? = transferRepository.findByReversalOfTransferIdAndUserId(transferId, userId)

    override fun getAllByUserId(userId: UUID): List<Transfer> =
        transferRepository.findAllByUserId(userId)

    override fun save(newTransfer: NewTransfer): Transfer =
        transferRepository.insert(
            Transfer(
                id = UUID.randomUUID(),
                userId = newTransfer.userId,
                sourceAccountId = newTransfer.sourceAccountId,
                destinationAccountId = newTransfer.destinationAccountId,
                currency = newTransfer.currency,
                amount = newTransfer.amount,
                note = newTransfer.note,
                occurredAt = newTransfer.occurredAt,
                createdAt = OffsetDateTime.now(clock),
                reversalOfTransferId = newTransfer.reversalOfTransferId,
            ),
        )
}
