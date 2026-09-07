package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.enums.Currency
import ru.digitalhustle.certis.model.goal.GoalFilter
import ru.digitalhustle.certis.model.goal.GoalPage
import ru.digitalhustle.certis.model.goal.GoalView
import java.util.UUID

interface GoalQueryService {

    fun getById(id: UUID, userId: UUID): GoalView

    fun getPage(userId: UUID, currency: Currency, filter: GoalFilter): GoalPage

    fun getAllActive(userId: UUID, currency: Currency): List<GoalView>

    fun getAll(userId: UUID, currency: Currency): List<GoalView>
}
