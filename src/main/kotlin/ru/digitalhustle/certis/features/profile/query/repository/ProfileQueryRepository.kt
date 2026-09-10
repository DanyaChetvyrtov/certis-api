package ru.digitalhustle.certis.features.profile.query.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.profile.model.Profile
import java.util.UUID

@Repository
class ProfileQueryRepository(
    private val dsl: DSLContext,
) {

    fun findById(id: UUID): Profile? =
        dsl.selectFrom(Tables.PROFILES)
            .where(Tables.PROFILES.ID.eq(id))
            .fetchOneInto(Profile::class.java)
}
