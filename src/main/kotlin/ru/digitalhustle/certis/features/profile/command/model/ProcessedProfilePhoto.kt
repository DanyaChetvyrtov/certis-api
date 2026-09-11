package ru.digitalhustle.certis.features.profile.command.model

data class ProcessedProfilePhoto(

    val meta: NewProfilePhotoMeta,

    val objectName: String,

    val contentType: String,
)
