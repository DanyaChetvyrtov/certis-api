package ru.digitalhustle.certis.features.profile.query.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import java.util.UUID

@Repository
class ProfilePhotoMetaQueryRepository(
    private val dsl: DSLContext,
) {

    fun findById(id: UUID): ProfilePhotoMeta? =
        dsl.selectFrom(Tables.PROFILE_PHOTOS)
            .where(Tables.PROFILE_PHOTOS.ID.eq(id))
            .fetchOneInto(ProfilePhotoMeta::class.java)

    fun findByProfileId(profileId: UUID): ProfilePhotoMeta? =
        dsl.selectFrom(Tables.PROFILE_PHOTOS)
            .where(Tables.PROFILE_PHOTOS.PROFILE_ID.eq(profileId))
            .fetchOneInto(ProfilePhotoMeta::class.java)
}
