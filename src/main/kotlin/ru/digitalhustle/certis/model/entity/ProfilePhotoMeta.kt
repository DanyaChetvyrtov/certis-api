package ru.digitalhustle.certis.model.entity

import java.time.OffsetDateTime
import java.util.UUID

data class ProfilePhotoMeta(

    val id: UUID,

    val profileId: UUID,

    val originalFileName: String,

    val extension: String,

    val fileSize: Long,

    val width: Int,

    val height: Int,

    val contentType: String,

    val url: String,

    val uploadedAt: OffsetDateTime,
)
