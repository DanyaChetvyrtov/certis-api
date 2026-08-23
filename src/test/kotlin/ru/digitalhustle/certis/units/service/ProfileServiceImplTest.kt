package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.digitalhustle.certis.exception.custom.EntityAlreadyExistsException
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.Profile
import ru.digitalhustle.certis.model.profile.NewProfile
import ru.digitalhustle.certis.model.profile.UpdateProfileData
import ru.digitalhustle.certis.repository.ProfileRepository
import ru.digitalhustle.certis.service.domain.impl.ProfileServiceImpl
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ProfileServiceImplTest {

    private val profileRepository = mock(ProfileRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC)

    private val profileService = ProfileServiceImpl(profileRepository, clock)

    private companion object {
        private const val NAME = "John"
        private const val SURNAME = "Doe"
    }

    @Test
    fun `should get profile by id`() {
        // given
        val profile = createProfile()

        `when`(profileRepository.findById(profile.id))
            .thenReturn(profile)

        // when
        val foundProfile = profileService.getById(profile.id)

        // then
        assertThat(foundProfile).isEqualTo(profile)

        verify(profileRepository)
            .findById(profile.id)
    }

    @Test
    fun `should throw not found exception when profile is not found by id`() {
        // given
        val id = UUID.randomUUID()

        `when`(profileRepository.findById(id))
            .thenReturn(null)

        // when, then
        assertThatThrownBy {
            profileService.getById(id)
        }.isInstanceOf(NotFoundException::class.java)

        verify(profileRepository)
            .findById(id)
    }

    @Test
    fun `should check if profile exists`() {
        // given
        val id = UUID.randomUUID()

        `when`(profileRepository.existsById(id))
            .thenReturn(true)

        // when
        val exists = profileService.exists(id)

        // then
        assertThat(exists).isTrue()

        verify(profileRepository)
            .existsById(id)
    }

    @Test
    fun `should save profile`() {
        // given
        val newProfile = createNewProfile()
        val savedProfile = createProfile(id = newProfile.id)

        `when`(profileRepository.existsById(newProfile.id))
            .thenReturn(false)

        `when`(profileRepository.save(anyProfile()))
            .thenReturn(savedProfile)

        // when
        val profile = profileService.save(newProfile)

        // then
        assertThat(profile).isEqualTo(savedProfile)

        val profileCaptor = ArgumentCaptor.forClass(Profile::class.java)
        verify(profileRepository)
            .save(captureProfile(profileCaptor))

        assertAll(
            { assertThat(profileCaptor.value.id).isEqualTo(newProfile.id) },
            { assertThat(profileCaptor.value.name).isEqualTo(newProfile.name) },
            { assertThat(profileCaptor.value.surname).isEqualTo(newProfile.surname) },
            { assertThat(profileCaptor.value.dateOfBirth).isEqualTo(newProfile.dateOfBirth) },
            { assertThat(profileCaptor.value.updatedAt).isEqualTo(OffsetDateTime.now(clock)) },
        )
    }

    @Test
    fun `should throw already exists exception when saving existing profile`() {
        // given
        val newProfile = createNewProfile()

        `when`(profileRepository.existsById(newProfile.id))
            .thenReturn(true)

        // when, then
        assertThatThrownBy {
            profileService.save(newProfile)
        }.isInstanceOf(EntityAlreadyExistsException::class.java)

        verify(profileRepository, never())
            .save(anyProfile())
    }

    @Test
    fun `should update profile`() {
        // given
        val updateProfileData = createUpdateProfileData()
        val updatedProfile = createProfile(id = updateProfileData.id)

        `when`(profileRepository.existsById(updateProfileData.id))
            .thenReturn(true)

        `when`(profileRepository.save(anyProfile()))
            .thenReturn(updatedProfile)

        // when
        val profile = profileService.update(updateProfileData)

        // then
        assertThat(profile).isEqualTo(updatedProfile)

        val profileCaptor = ArgumentCaptor.forClass(Profile::class.java)
        verify(profileRepository)
            .save(captureProfile(profileCaptor))

        assertAll(
            { assertThat(profileCaptor.value.id).isEqualTo(updateProfileData.id) },
            { assertThat(profileCaptor.value.name).isEqualTo(updateProfileData.name) },
            { assertThat(profileCaptor.value.surname).isEqualTo(updateProfileData.surname) },
            { assertThat(profileCaptor.value.dateOfBirth).isEqualTo(updateProfileData.dateOfBirth) },
            { assertThat(profileCaptor.value.updatedAt).isEqualTo(OffsetDateTime.now(clock)) },
        )
    }

    @Test
    fun `should throw not found exception when updating missing profile`() {
        // given
        val updateProfileData = createUpdateProfileData()

        `when`(profileRepository.existsById(updateProfileData.id))
            .thenReturn(false)

        // when, then
        assertThatThrownBy {
            profileService.update(updateProfileData)
        }.isInstanceOf(NotFoundException::class.java)

        verify(profileRepository, never())
            .save(anyProfile())
    }

    @Test
    fun `should delete profile`() {
        // given
        val id = UUID.randomUUID()

        `when`(profileRepository.existsById(id))
            .thenReturn(true)

        // when
        profileService.delete(id)

        // then
        verify(profileRepository)
            .deleteById(id)
    }

    @Test
    fun `should throw not found exception when deleting missing profile`() {
        // given
        val id = UUID.randomUUID()

        `when`(profileRepository.existsById(id))
            .thenReturn(false)

        // when, then
        assertThatThrownBy {
            profileService.delete(id)
        }.isInstanceOf(NotFoundException::class.java)

        verify(profileRepository, never())
            .deleteById(id)
    }

    private fun createNewProfile(
        id: UUID = UUID.randomUUID(),
        name: String = NAME,
        surname: String = SURNAME,
        dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1),
    ): NewProfile =
        NewProfile(
            id = id,
            name = name,
            surname = surname,
            dateOfBirth = dateOfBirth,
        )

    private fun createUpdateProfileData(
        id: UUID = UUID.randomUUID(),
        name: String = NAME,
        surname: String = SURNAME,
        dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1),
    ): UpdateProfileData =
        UpdateProfileData(
            id = id,
            name = name,
            surname = surname,
            dateOfBirth = dateOfBirth,
        )

    private fun createProfile(
        id: UUID = UUID.randomUUID(),
        name: String = NAME,
        surname: String = SURNAME,
        dateOfBirth: LocalDate = LocalDate.of(2000, 1, 1),
    ): Profile =
        Profile(
            id = id,
            name = name,
            surname = surname,
            dateOfBirth = dateOfBirth,
            updatedAt = OffsetDateTime.now(),
        )

    private fun anyProfile(): Profile {
        org.mockito.Mockito.any<Profile>()
        return createProfile()
    }

    private fun captureProfile(captor: ArgumentCaptor<Profile>): Profile {
        captor.capture()
        return createProfile()
    }
}
