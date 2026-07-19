package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.entity.Transfer
import ru.digitalhustle.certis.model.transfer.NewTransfer
import java.util.UUID

interface TransferService {

    fun getById(id: UUID, userId: UUID): Transfer

    fun getByIdForUpdate(id: UUID, userId: UUID): Transfer

    fun findReversal(transferId: UUID, userId: UUID): Transfer?

    fun getAllByUserId(userId: UUID): List<Transfer>

    fun save(newTransfer: NewTransfer): Transfer
}
