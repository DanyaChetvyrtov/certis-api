package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.api.dto.PhotoMetaInfoDto
import ru.digitalhustle.certis.api.dto.ProfileDto
import ru.digitalhustle.certis.api.dto.request.CreateProfileRq
import ru.digitalhustle.certis.api.dto.request.UpdateProfileRq
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.profile.command.model.NewProfile
import ru.digitalhustle.certis.features.profile.command.model.UpdateProfileData
import ru.digitalhustle.certis.features.profile.model.ProfilePhotoMeta
import ru.digitalhustle.certis.features.profile.model.ProfilePreview
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface ProfileMapper {

    fun convert(source: CreateProfileRq, id: UUID): NewProfile

    fun convert(source: UpdateProfileRq, id: UUID): UpdateProfileData

    fun convert(source: ProfilePreview): ProfileDto

    fun convert(source: ProfilePhotoMeta): PhotoMetaInfoDto
}
