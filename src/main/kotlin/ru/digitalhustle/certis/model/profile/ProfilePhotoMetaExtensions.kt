package ru.digitalhustle.certis.model.profile

import ru.digitalhustle.certis.model.entity.ProfilePhotoMeta

val ProfilePhotoMeta.objectName: String
    get() = "$id.$extension"
