package ru.digitalhustle.certis.model.entity

import java.time.LocalDateTime
import java.util.UUID

data class User(

    val id: UUID,

    val email: String,

    val passwordHash: String,

    val lastLogin: LocalDateTime,

    val createdAt: LocalDateTime,
)
