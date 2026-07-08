package ru.digitalhustle.certis.repository;

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.model.entity.User
import java.util.*

@Repository
class UserRepository(
    private val dsl: DSLContext
) {

    fun findByEmail(email: String): User? =
        dsl.selectFrom(Tables.USERS)
            .where(Tables.USERS.EMAIL.eq(email))
            .fetchOneInto(User::class.java)

    fun findById(id: UUID): User? =
        dsl.selectFrom(Tables.USERS)
            .where(Tables.USERS.ID.eq(id))
            .fetchOneInto(User::class.java)

    fun save(user: User): User =
        dsl.insertInto(Tables.USERS)
            .set(dsl.newRecord(Tables.USERS, user))
            .onConflict(Tables.USERS.ID)
            .doUpdate()
            .set(dsl.newRecord(Tables.USERS, user))
            .returning()
            .fetchOneInto(User::class.java)!!

    fun saveAll(users: List<User>): List<User> {
        val records = users.map {
            dsl.newRecord(Tables.USERS, it)
        }

        dsl.batchStore(records).execute()
        return users
    }

    fun deleteById(id: UUID) {
        dsl.deleteFrom(Tables.USERS)
            .where(Tables.USERS.ID.eq(id))
            .execute()
    }
}