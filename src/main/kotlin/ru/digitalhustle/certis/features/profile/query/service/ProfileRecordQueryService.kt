package ru.digitalhustle.certis.features.profile.query.service

import ru.digitalhustle.certis.features.profile.model.Profile
import java.util.UUID

interface ProfileRecordQueryService {

    fun getById(id: UUID): Profile
}
