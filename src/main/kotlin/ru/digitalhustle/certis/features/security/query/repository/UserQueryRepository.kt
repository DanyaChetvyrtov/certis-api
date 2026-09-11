package ru.digitalhustle.certis.features.security.query.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.security.model.User
import java.util.UUID

@Repository
class UserQueryRepository(
    private val dsl: DSLContext,
) {

    fun findById(id: UUID): User? =
        dsl.selectFrom(Tables.USERS)
            .where(Tables.USERS.ID.eq(id))
            .fetchOneInto(User::class.java)

    fun findByEmail(email: String): User? =
        dsl.selectFrom(Tables.USERS)
            .where(Tables.USERS.EMAIL.eq(email))
            .fetchOneInto(User::class.java)

    fun findPreferredCurrency(id: UUID): Currency? =
        dsl.select(Tables.USERS.PREFERRED_CURRENCY)
            .from(Tables.USERS)
            .where(Tables.USERS.ID.eq(id))
            .fetchOne(Tables.USERS.PREFERRED_CURRENCY)
            ?.let(Currency::valueOf)
}
