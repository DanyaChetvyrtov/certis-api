package ru.digitalhustle.certis.features.transaction.command.service

import ru.digitalhustle.certis.features.transaction.command.model.CreateTransferData
import ru.digitalhustle.certis.features.transaction.command.model.ReverseTransferData
import ru.digitalhustle.certis.features.transaction.model.Transfer

interface TransferCommandService {

    fun save(transfer: CreateTransferData): Transfer

    fun reverse(transfer: ReverseTransferData): Transfer
}
