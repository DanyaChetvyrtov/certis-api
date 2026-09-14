package ru.digitalhustle.certis.features.transaction.query.service

import ru.digitalhustle.certis.features.transaction.model.Transfer
import java.util.UUID

interface TransferQueryService {

    fun getById(id: UUID, userId: UUID): Transfer

    fun getAllByUserId(userId: UUID): List<Transfer>
}
