package ru.digitalhustle.certis.api.dto.response

import ru.digitalhustle.certis.api.dto.AuthSessionDto

data class SessionsRs(

    val sessions: List<AuthSessionDto>,
)
