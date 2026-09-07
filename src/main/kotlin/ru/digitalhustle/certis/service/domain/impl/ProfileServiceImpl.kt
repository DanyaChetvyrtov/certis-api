package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.Profile
import ru.digitalhustle.certis.model.profile.NewProfile
import ru.digitalhustle.certis.model.profile.UpdateProfileData
import ru.digitalhustle.certis.repository.ProfileRepository
import ru.digitalhustle.certis.service.domain.ProfileService
import ru.digitalhustle.certis.time.ApplicationClock
import java.util.UUID

@Service
class ProfileServiceImpl(
    private val profileRepository: ProfileRepository,
    private val applicationClock: ApplicationClock,
) : ProfileService {

    override fun getById(id: UUID): Profile =
        profileRepository.findById(id)
            ?: throw NotFoundException.entity("Profile")

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
