package ru.digitalhustle.certis.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.model.account.UpdateAccountData
import ru.digitalhustle.certis.model.entity.Account
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class AccountRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Account? =
        dsl.selectFrom(Tables.ACCOUNTS)
            .where(
                Tables.ACCOUNTS.ID.eq(id)
                    .and(Tables.ACCOUNTS.USER_ID.eq(userId)),
            )
            .fetchOneInto(Account::class.java)

    fun findByIdAndUserIdForShare(
        id: UUID,
        userId: UUID,
    ): Account? =
        dsl.selectFrom(Tables.ACCOUNTS)
            .where(
                Tables.ACCOUNTS.ID.eq(id)
                    .and(Tables.ACCOUNTS.USER_ID.eq(userId)),
            )
            .forShare()
            .fetchOneInto(Account::class.java)

    fun findByIdAndUserIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Account? =
        dsl.selectFrom(Tables.ACCOUNTS)
            .where(
                Tables.ACCOUNTS.ID.eq(id)
                    .and(Tables.ACCOUNTS.USER_ID.eq(userId)),
            )
            .forUpdate()
            .fetchOneInto(Account::class.java)

    fun findAllByIdsAndUserIdForShare(
        ids: Collection<UUID>,
        userId: UUID,
    ): List<Account> =
        dsl.selectFrom(Tables.ACCOUNTS)
            .where(
                Tables.ACCOUNTS.ID.`in`(ids)
                    .and(Tables.ACCOUNTS.USER_ID.eq(userId)),
            )
            .orderBy(Tables.ACCOUNTS.ID)
            .forShare()
            .fetchInto(Account::class.java)

    fun findAllByUserId(userId: UUID): List<Account> =
        dsl.selectFrom(Tables.ACCOUNTS)
            .where(Tables.ACCOUNTS.USER_ID.eq(userId))
            .orderBy(
                Tables.ACCOUNTS.CLOSED_AT.asc().nullsFirst(),
                Tables.ACCOUNTS.CREATED_AT.desc(),
            )
            .fetchInto(Account::class.java)

    fun insert(account: Account): Account =
        dsl.insertInto(Tables.ACCOUNTS)
            .set(dsl.newRecord(Tables.ACCOUNTS, account))
            .returning()
            .fetchOneInto(Account::class.java)!!

    fun updateActive(account: UpdateAccountData): Account? =
        dsl.update(Tables.ACCOUNTS)
            .set(Tables.ACCOUNTS.NAME, account.name)
            .set(Tables.ACCOUNTS.TYPE, account.type.name)
            .set(Tables.ACCOUNTS.OPENING_BALANCE, account.openingBalance)
            .where(
                Tables.ACCOUNTS.ID.eq(account.id)
                    .and(Tables.ACCOUNTS.USER_ID.eq(account.userId))
                    .and(Tables.ACCOUNTS.CLOSED_AT.isNull()),
            )
            .returning()
            .fetchOneInto(Account::class.java)

    fun close(
        id: UUID,
        userId: UUID,
        closedAt: OffsetDateTime,
    ): Boolean =
        dsl.update(Tables.ACCOUNTS)
            .set(Tables.ACCOUNTS.CLOSED_AT, closedAt)
            .where(
                Tables.ACCOUNTS.ID.eq(id)
                    .and(Tables.ACCOUNTS.USER_ID.eq(userId))
                    .and(Tables.ACCOUNTS.CLOSED_AT.isNull()),
            )
            .execute() > 0
}
