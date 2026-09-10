package ru.digitalhustle.certis.features.profile.application.service

import java.util.UUID

interface ProfilePhotoSupport {

    fun getPhotoUrl(profileId: UUID): String?

    fun deletePhoto(profileId: UUID)
}
