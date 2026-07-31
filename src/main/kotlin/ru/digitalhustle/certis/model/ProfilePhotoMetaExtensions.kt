package ru.digitalhustle.certis.model

import ru.digitalhustle.certis.model.entity.ProfilePhotoMeta

val ProfilePhotoMeta.objectName: String
    get() = "$id.$extension"
