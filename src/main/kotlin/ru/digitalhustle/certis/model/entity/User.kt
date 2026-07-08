package ru.digitalhustle.certis.model.entity;

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import ru.digitalhustle.certis.enums.Currency
import java.time.LocalDateTime
import java.util.*

@Table(name = "users", schema = "keeper")
data class User(

    @Id
    val id: UUID,

    @Column(name = "email", nullable = false)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    val password: String,

    @Column(name = "preferred_currency", nullable = false)
    val preferredCurrency: Currency,

    @Column(name = "last_login")
    val lastLogin: LocalDateTime,

    @Column(name = "created_at")
    val createdAt: LocalDateTime,
)
