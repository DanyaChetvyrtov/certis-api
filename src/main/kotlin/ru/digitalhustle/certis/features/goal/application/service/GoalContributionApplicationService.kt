package ru.digitalhustle.certis.features.goal.application.service

import ru.digitalhustle.certis.features.goal.model.CreateGoalContributionData
import ru.digitalhustle.certis.features.goal.model.GoalContributionResult
import ru.digitalhustle.certis.features.goal.model.GoalProgress
import java.util.UUID

interface GoalContributionApplicationService {

    fun contribute(data: CreateGoalContributionData): GoalContributionResult

    fun refundContribution(goalId: UUID, contributionId: UUID, userId: UUID): GoalProgress
}
