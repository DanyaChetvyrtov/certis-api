package ru.digitalhustle.certis.features.transaction.query.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.transaction.model.Transfer
import java.util.UUID

@Repository
class TransferQueryRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Transfer? =
        dsl.selectFrom(Tables.TRANSFERS)
            .where(
                Tables.TRANSFERS.ID.eq(id)
                    .and(Tables.TRANSFERS.USER_ID.eq(userId)),
            )
            .fetchOneInto(Transfer::class.java)

    fun findAllByUserId(userId: UUID): List<Transfer> =
        dsl.selectFrom(Tables.TRANSFERS)
            .where(Tables.TRANSFERS.USER_ID.eq(userId))
            .orderBy(
                Tables.TRANSFERS.OCCURRED_AT.desc(),
                Tables.TRANSFERS.CREATED_AT.desc(),
                Tables.TRANSFERS.ID.desc(),
            )
            .fetchInto(Transfer::class.java)
}
