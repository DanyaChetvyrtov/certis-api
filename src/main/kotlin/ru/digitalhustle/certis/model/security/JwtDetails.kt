package ru.digitalhustle.certis.model.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class JwtDetails(
    val id: UUID,
    private val username: String,
    private val password: String,
) : UserDetails {

    override fun getUsername() = username

    override fun getPassword() = password

    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
}
