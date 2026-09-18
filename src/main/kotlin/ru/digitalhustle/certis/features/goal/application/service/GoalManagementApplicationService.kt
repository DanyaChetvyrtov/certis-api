package ru.digitalhustle.certis.features.goal.application.service

import ru.digitalhustle.certis.features.goal.model.CreateGoalData
import ru.digitalhustle.certis.features.goal.model.GoalView
import ru.digitalhustle.certis.features.goal.model.UpdateGoalData
import java.util.UUID

interface GoalManagementApplicationService {

    fun create(data: CreateGoalData): GoalView

    fun update(data: UpdateGoalData): GoalView

    fun cancel(goalId: UUID, userId: UUID)
}
