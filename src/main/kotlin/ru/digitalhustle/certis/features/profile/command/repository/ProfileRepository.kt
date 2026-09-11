package ru.digitalhustle.certis.features.profile.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.profile.model.Profile
import java.util.UUID

@Repository
class ProfileRepository(
    private val dsl: DSLContext,
) {

    fun existsById(id: UUID): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(Tables.PROFILES)
                .where(Tables.PROFILES.ID.eq(id)),
        )

    fun save(profile: Profile): Profile =
        dsl.insertInto(Tables.PROFILES)
            .set(dsl.newRecord(Tables.PROFILES, profile))
            .onConflict(Tables.PROFILES.ID)
            .doUpdate()
            .set(dsl.newRecord(Tables.PROFILES, profile))
            .returning()
            .fetchSingleInto(Profile::class.java)

    fun deleteById(id: UUID) {
        dsl.deleteFrom(Tables.PROFILES)
            .where(Tables.PROFILES.ID.eq(id))
            .execute()
    }
}
