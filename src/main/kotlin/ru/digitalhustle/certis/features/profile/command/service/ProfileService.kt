package ru.digitalhustle.certis.features.profile.command.service

import ru.digitalhustle.certis.features.profile.command.model.NewProfile
import ru.digitalhustle.certis.features.profile.command.model.UpdateProfileData
import ru.digitalhustle.certis.features.profile.model.Profile
import java.util.UUID

interface ProfileService {

    fun exists(profileId: UUID): Boolean

    fun save(profile: NewProfile): Profile

    fun update(profile: UpdateProfileData): Profile

    fun delete(id: UUID)
}
