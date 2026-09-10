package ru.digitalhustle.certis.features.profile.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.profile.application.service.ProfileManagementApplicationService
import ru.digitalhustle.certis.features.profile.application.service.ProfilePhotoSupport
import ru.digitalhustle.certis.features.profile.command.model.NewProfile
import ru.digitalhustle.certis.features.profile.command.model.UpdateProfileData
import ru.digitalhustle.certis.features.profile.command.service.ProfileService
import ru.digitalhustle.certis.features.profile.model.ProfilePreview
import ru.digitalhustle.certis.features.profile.model.toPreview
import ru.digitalhustle.certis.features.security.api.UserPreferencesCommand
import ru.digitalhustle.certis.features.security.api.UserPreferencesQuery
import ru.digitalhustle.certis.shared.enums.Currency
import java.util.UUID

@Service
class ProfileManagementApplicationServiceImpl(
    private val profileService: ProfileService,
    private val userPreferencesCommand: UserPreferencesCommand,
    private val userPreferencesQuery: UserPreferencesQuery,
    private val profilePhotoSupport: ProfilePhotoSupport,
) : ProfileManagementApplicationService {

    @Transactional
    override fun saveProfile(
        profile: NewProfile,
        preferredCurrency: Currency,
    ): ProfilePreview {
        val savedProfile = profileService.save(profile)
        userPreferencesCommand.updatePreferredCurrency(savedProfile.id, preferredCurrency)

        return savedProfile.toPreview(
            photoUrl = null,
            preferredCurrency = preferredCurrency,
        )
    }

    @Transactional
    override fun updateProfile(
        profile: UpdateProfileData,
        preferredCurrency: Currency?,
    ): ProfilePreview {
        val updatedProfile = profileService.update(profile)
        val effectiveCurrency = preferredCurrency ?: userPreferencesQuery.getPreferredCurrency(updatedProfile.id)

        preferredCurrency?.let {
            userPreferencesCommand.updatePreferredCurrency(updatedProfile.id, it)
        }

        return updatedProfile.toPreview(
            photoUrl = profilePhotoSupport.getPhotoUrl(updatedProfile.id),
            preferredCurrency = effectiveCurrency,
        )
    }

    @Transactional
    override fun deleteProfile(profileId: UUID) {
        if (!profileService.exists(profileId)) {
            return
        }

        profilePhotoSupport.deletePhoto(profileId)
        profileService.delete(profileId)
    }
}
