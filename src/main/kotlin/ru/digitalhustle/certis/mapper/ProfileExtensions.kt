package ru.digitalhustle.certis.mapper

import ru.digitalhustle.certis.model.ProfilePreview
import ru.digitalhustle.certis.model.entity.Profile

fun Profile.toPreview(photoUrl: String?): ProfilePreview =
    ProfilePreview(
        id = id,
        name = name,
        surname = surname,
        dateOfBirth = dateOfBirth,
        photoUrl = photoUrl,
    )
