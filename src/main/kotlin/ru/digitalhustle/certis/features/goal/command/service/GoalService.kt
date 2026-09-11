package ru.digitalhustle.certis.features.goal.command.service

import ru.digitalhustle.certis.features.goal.model.Goal
import ru.digitalhustle.certis.features.goal.model.NewGoal
import java.util.UUID

interface GoalService {

    fun getById(id: UUID, userId: UUID): Goal

    fun getByIdForUpdate(id: UUID, userId: UUID): Goal

    fun save(goal: NewGoal): Goal

    fun update(goal: Goal): Goal
}
