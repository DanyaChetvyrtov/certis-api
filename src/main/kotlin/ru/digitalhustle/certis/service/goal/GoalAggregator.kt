package ru.digitalhustle.certis.service.goal

import ru.digitalhustle.certis.model.goal.CreateGoalContributionData
import ru.digitalhustle.certis.model.goal.CreateGoalData
import ru.digitalhustle.certis.model.goal.GoalContributionFilter
import ru.digitalhustle.certis.model.goal.GoalContributionPage
import ru.digitalhustle.certis.model.goal.GoalContributionResult
import ru.digitalhustle.certis.model.goal.GoalFilter
import ru.digitalhustle.certis.model.goal.GoalOverview
import ru.digitalhustle.certis.model.goal.GoalOverviewFilter
import ru.digitalhustle.certis.model.goal.GoalPage
import ru.digitalhustle.certis.model.goal.GoalPlanPreview
import ru.digitalhustle.certis.model.goal.GoalPlanPreviewData
import ru.digitalhustle.certis.model.goal.GoalProgress
import ru.digitalhustle.certis.model.goal.GoalView
import ru.digitalhustle.certis.model.goal.UpdateGoalData
import java.util.UUID

interface GoalAggregator {

    fun getPage(userId: UUID, filter: GoalFilter): GoalPage

    fun getOverview(userId: UUID, filter: GoalOverviewFilter): GoalOverview

    fun preview(data: GoalPlanPreviewData): GoalPlanPreview

    fun getById(id: UUID, userId: UUID): GoalView

    fun create(data: CreateGoalData): GoalView

    fun update(data: UpdateGoalData): GoalView

    fun contribute(data: CreateGoalContributionData): GoalContributionResult

    fun getContributions(goalId: UUID, userId: UUID, filter: GoalContributionFilter): GoalContributionPage

    fun refundContribution(goalId: UUID, contributionId: UUID, userId: UUID): GoalProgress

    fun cancel(goalId: UUID, userId: UUID)
}
