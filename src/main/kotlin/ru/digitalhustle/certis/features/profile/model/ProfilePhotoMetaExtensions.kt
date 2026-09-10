package ru.digitalhustle.certis.features.profile.model

val ProfilePhotoMeta.objectName: String
    get() = "$id.$extension"
