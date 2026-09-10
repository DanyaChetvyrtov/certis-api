package ru.digitalhustle.certis.features.security.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.security.model.User
import java.util.UUID

@Repository
class UserRepository(
    private val dsl: DSLContext,
) {

    fun findById(id: UUID): User? =
        dsl.selectFrom(Tables.USERS)
            .where(Tables.USERS.ID.eq(id))
            .fetchOneInto(User::class.java)

    fun create(user: User): User? =
        dsl.insertInto(Tables.USERS)
            .set(dsl.newRecord(Tables.USERS, user))
            .onConflict(Tables.USERS.EMAIL)
            .doNothing()
            .returning()
            .fetchOneInto(User::class.java)

    fun save(user: User): User =
        dsl.insertInto(Tables.USERS)
            .set(dsl.newRecord(Tables.USERS, user))
            .onConflict(Tables.USERS.ID)
            .doUpdate()
            .set(dsl.newRecord(Tables.USERS, user))
            .returning()
            .fetchSingleInto(User::class.java)

    fun deleteById(id: UUID) {
        dsl.deleteFrom(Tables.USERS)
            .where(Tables.USERS.ID.eq(id))
            .execute()
    }

    fun findPreferredCurrencyForUpdate(userId: UUID): Currency? =
        dsl.select(Tables.USERS.PREFERRED_CURRENCY)
            .from(Tables.USERS)
            .where(Tables.USERS.ID.eq(userId))
            .forUpdate()
            .fetchOne(Tables.USERS.PREFERRED_CURRENCY)
            ?.let(Currency::valueOf)
}
