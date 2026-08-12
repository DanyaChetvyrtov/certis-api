package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.entity.Profile
import ru.digitalhustle.certis.model.profile.NewProfile
import ru.digitalhustle.certis.model.profile.UpdateProfileData
import java.util.UUID

interface ProfileService {

    fun getById(id: UUID): Profile

    fun exists(profileId: UUID): Boolean

    fun save(profile: NewProfile): Profile

    fun update(profile: UpdateProfileData): Profile

    fun delete(id: UUID)
}
