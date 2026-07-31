package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.NewProfile
import ru.digitalhustle.certis.model.UpdateProfileData
import ru.digitalhustle.certis.model.entity.Profile
import java.util.UUID

interface ProfileService {

    fun getById(id: UUID): Profile

    fun exists(profileId: UUID): Boolean

    fun save(profile: NewProfile): Profile

    fun update(profile: UpdateProfileData): Profile

    fun delete(id: UUID)
}
