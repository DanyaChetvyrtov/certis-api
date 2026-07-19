package ru.digitalhustle.certis.features.profile.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import java.util.UUID

@Repository
class ProfilePhotoMetaRepository(
    private val dsl: DSLContext,
) {

    fun findByProfileId(profileId: UUID): ProfilePhotoMeta? =
        dsl.selectFrom(Tables.PROFILE_PHOTOS)
            .where(Tables.PROFILE_PHOTOS.PROFILE_ID.eq(profileId))
            .fetchOneInto(ProfilePhotoMeta::class.java)

    fun save(profile: ProfilePhotoMeta): ProfilePhotoMeta =
        dsl.insertInto(Tables.PROFILE_PHOTOS)
            .set(dsl.newRecord(Tables.PROFILE_PHOTOS, profile))
            .onConflict(Tables.PROFILE_PHOTOS.ID)
            .doUpdate()
            .set(dsl.newRecord(Tables.PROFILE_PHOTOS, profile))
            .returning()
            .fetchSingleInto(ProfilePhotoMeta::class.java)

    fun deleteByProfileId(id: UUID) {
        dsl.deleteFrom(Tables.PROFILE_PHOTOS)
            .where(Tables.PROFILE_PHOTOS.PROFILE_ID.eq(id))
            .execute()
    }
}
