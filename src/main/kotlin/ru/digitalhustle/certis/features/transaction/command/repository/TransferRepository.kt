package ru.digitalhustle.certis.features.transaction.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.transaction.model.Transfer
import java.util.UUID

@Repository
class TransferRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Transfer? =
        dsl.selectFrom(Tables.TRANSFERS)
            .where(
                Tables.TRANSFERS.ID.eq(id)
                    .and(Tables.TRANSFERS.USER_ID.eq(userId)),
            )
            .forUpdate()
            .fetchOneInto(Transfer::class.java)

    fun findByReversalOfTransferIdAndUserId(
        transferId: UUID,
        userId: UUID,
    ): Transfer? =
        dsl.selectFrom(Tables.TRANSFERS)
            .where(
                Tables.TRANSFERS.REVERSAL_OF_TRANSFER_ID.eq(transferId)
                    .and(Tables.TRANSFERS.USER_ID.eq(userId)),
            )
            .fetchOneInto(Transfer::class.java)

    fun insert(transfer: Transfer): Transfer =
        dsl.insertInto(Tables.TRANSFERS)
            .set(dsl.newRecord(Tables.TRANSFERS, transfer))
            .returning()
            .fetchOneInto(Transfer::class.java)
            ?: error("Transfer insert returned no row")
}
