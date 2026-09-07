package ru.digitalhustle.certis.service.domain

import ru.digitalhustle.certis.model.entity.Goal
import ru.digitalhustle.certis.model.goal.NewGoal
import java.util.UUID

interface GoalService {

    fun getById(id: UUID, userId: UUID): Goal

    fun getByIdForUpdate(id: UUID, userId: UUID): Goal

    fun save(goal: NewGoal): Goal

    fun update(goal: Goal): Goal
}
