package ru.digitalhustle.certis.model

import java.util.UUID

data class NewProfilePhotoMeta(

    val id: UUID,

    val profileId: UUID,

    val originalFileName: String,

    val extension: String,

    val fileSize: Long,

    val width: Int,

    val height: Int,

    val contentType: String,

    val url: String,
)
