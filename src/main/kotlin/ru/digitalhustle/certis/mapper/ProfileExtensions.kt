package ru.digitalhustle.certis.mapper

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.model.entity.Profile
import ru.digitalhustle.certis.model.profile.ProfilePreview

fun Profile.toPreview(
    photoUrl: String?,
    preferredCurrency: Currency,
): ProfilePreview =
    ProfilePreview(
        id = id,
        name = name,
        surname = surname,
        dateOfBirth = dateOfBirth,
        preferredCurrency = preferredCurrency,
        photoUrl = photoUrl,
    )
