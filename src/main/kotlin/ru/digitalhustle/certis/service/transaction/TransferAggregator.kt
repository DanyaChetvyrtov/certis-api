package ru.digitalhustle.certis.service.transaction

import ru.digitalhustle.certis.model.entity.Transfer
import ru.digitalhustle.certis.model.transfer.CreateTransferData
import ru.digitalhustle.certis.model.transfer.ReverseTransferData
import java.util.UUID

interface TransferAggregator {

    fun getById(id: UUID, userId: UUID): Transfer

    fun getAllByUserId(userId: UUID): List<Transfer>

    fun save(transfer: CreateTransferData): Transfer

    fun reverse(transfer: ReverseTransferData): Transfer
}
