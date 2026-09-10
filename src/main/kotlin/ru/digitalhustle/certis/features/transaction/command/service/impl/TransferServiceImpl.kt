package ru.digitalhustle.certis.features.transaction.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.transaction.command.model.NewTransfer
import ru.digitalhustle.certis.features.transaction.command.repository.TransferRepository
import ru.digitalhustle.certis.features.transaction.command.service.TransferService
import ru.digitalhustle.certis.features.transaction.model.Transfer
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
class TransferServiceImpl(
    private val transferRepository: TransferRepository,
    private val applicationClock: ApplicationClock,
) : TransferService {

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
                createdAt = applicationClock.now(),
                reversalOfTransferId = newTransfer.reversalOfTransferId,
            ),
        )
}
