package ru.digitalhustle.certis.features.goal.application.service

import ru.digitalhustle.certis.features.goal.model.Goal
import ru.digitalhustle.certis.features.goal.model.InitialGoalContributionData
import java.math.BigDecimal
import java.util.UUID

interface GoalContributionSupport {

    fun getSavedAmount(goalId: UUID, userId: UUID): BigDecimal

    fun saveInitial(goal: Goal, contribution: InitialGoalContributionData)
}
