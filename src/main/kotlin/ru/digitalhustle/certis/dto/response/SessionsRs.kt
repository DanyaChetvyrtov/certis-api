package ru.digitalhustle.certis.dto.response

import ru.digitalhustle.certis.dto.AuthSessionDto

data class SessionsRs(

    val sessions: List<AuthSessionDto>,
)
