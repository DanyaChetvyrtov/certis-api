package ru.digitalhustle.certis.features.goal.query.service

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.features.goal.model.GoalFilter
import ru.digitalhustle.certis.features.goal.model.GoalPage
import ru.digitalhustle.certis.features.goal.model.GoalView
import java.util.UUID

interface GoalQueryService {

    fun getById(id: UUID, userId: UUID): GoalView

    fun getPage(userId: UUID, currency: Currency, filter: GoalFilter): GoalPage

    fun getAllActive(userId: UUID, currency: Currency): List<GoalView>

    fun getAll(userId: UUID, currency: Currency): List<GoalView>

    fun requireOwned(id: UUID, userId: UUID)
}
