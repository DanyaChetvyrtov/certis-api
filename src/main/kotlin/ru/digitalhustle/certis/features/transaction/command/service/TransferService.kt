package ru.digitalhustle.certis.features.transaction.command.service

import ru.digitalhustle.certis.features.transaction.command.model.NewTransfer
import ru.digitalhustle.certis.features.transaction.model.Transfer
import java.util.UUID

interface TransferService {

    fun getByIdForUpdate(id: UUID, userId: UUID): Transfer

    fun findReversal(transferId: UUID, userId: UUID): Transfer?

    fun save(newTransfer: NewTransfer): Transfer
}
