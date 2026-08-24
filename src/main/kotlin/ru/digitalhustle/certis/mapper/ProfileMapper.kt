package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.PhotoMetaInfoDto
import ru.digitalhustle.certis.dto.ProfileDto
import ru.digitalhustle.certis.dto.request.CreateProfileRq
import ru.digitalhustle.certis.dto.request.UpdateProfileRq
import ru.digitalhustle.certis.model.entity.ProfilePhotoMeta
import ru.digitalhustle.certis.model.profile.NewProfile
import ru.digitalhustle.certis.model.profile.ProfilePreview
import ru.digitalhustle.certis.model.profile.UpdateProfileData
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface ProfileMapper {

    fun convert(source: CreateProfileRq, id: UUID): NewProfile

    fun convert(source: UpdateProfileRq, id: UUID): UpdateProfileData

    fun convert(source: ProfilePreview): ProfileDto

    fun convert(source: ProfilePhotoMeta): PhotoMetaInfoDto
}
