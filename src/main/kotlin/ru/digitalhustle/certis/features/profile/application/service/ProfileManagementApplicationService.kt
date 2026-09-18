package ru.digitalhustle.certis.features.profile.application.service

import ru.digitalhustle.certis.features.profile.command.model.NewProfile
import ru.digitalhustle.certis.features.profile.command.model.UpdateProfileData
import ru.digitalhustle.certis.features.profile.model.ProfilePreview
import ru.digitalhustle.certis.shared.enums.Currency
import java.util.UUID

interface ProfileManagementApplicationService {

    fun saveProfile(profile: NewProfile, preferredCurrency: Currency): ProfilePreview

    fun updateProfile(profile: UpdateProfileData, preferredCurrency: Currency?): ProfilePreview

    fun deleteProfile(profileId: UUID)
}
