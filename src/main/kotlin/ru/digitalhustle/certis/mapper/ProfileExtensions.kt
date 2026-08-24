package ru.digitalhustle.certis.mapper

import ru.digitalhustle.certis.model.entity.Profile
import ru.digitalhustle.certis.model.profile.ProfilePreview

fun Profile.toPreview(photoUrl: String?): ProfilePreview =
    ProfilePreview(
        id = id,
        name = name,
        surname = surname,
        dateOfBirth = dateOfBirth,
        photoUrl = photoUrl,
    )
