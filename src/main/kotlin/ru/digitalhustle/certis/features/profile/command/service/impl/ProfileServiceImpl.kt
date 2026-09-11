package ru.digitalhustle.certis.features.profile.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.profile.command.model.NewProfile
import ru.digitalhustle.certis.features.profile.command.model.UpdateProfileData
import ru.digitalhustle.certis.features.profile.command.repository.ProfileRepository
import ru.digitalhustle.certis.features.profile.command.service.ProfileService
import ru.digitalhustle.certis.features.profile.model.Profile
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
class ProfileServiceImpl(
    private val profileRepository: ProfileRepository,
    private val applicationClock: ApplicationClock,
) : ProfileService {

    override fun exists(profileId: UUID): Boolean = profileRepository.existsById(profileId)

    override fun save(profile: NewProfile): Profile {
        if (profileRepository.existsById(profile.id)) {
            throw EntityAlreadyExistsException.entity("Profile", "id")
        }

        return profileRepository.save(
            Profile(
                id = profile.id,
                name = profile.name,
                surname = profile.surname,
                dateOfBirth = profile.dateOfBirth,
                updatedAt = applicationClock.now(),
            ),
        )
    }

    override fun update(profile: UpdateProfileData): Profile {
        if (!exists(profile.id)) {
            throw NotFoundException.entity("Profile")
        }

        return profileRepository.save(
            Profile(
                id = profile.id,
                name = profile.name,
                surname = profile.surname,
                dateOfBirth = profile.dateOfBirth,
                updatedAt = applicationClock.now(),
            ),
        )
    }

    override fun delete(id: UUID) {
        if (!exists(id)) {
            throw NotFoundException.entity("Profile")
        }

        profileRepository.deleteById(id)
    }
}
