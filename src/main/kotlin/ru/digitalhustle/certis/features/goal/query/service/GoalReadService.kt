package ru.digitalhustle.certis.features.goal.query.service

import ru.digitalhustle.certis.features.goal.model.GoalContributionFilter
import ru.digitalhustle.certis.features.goal.model.GoalContributionPage
import ru.digitalhustle.certis.features.goal.model.GoalFilter
import ru.digitalhustle.certis.features.goal.model.GoalOverview
import ru.digitalhustle.certis.features.goal.model.GoalOverviewFilter
import ru.digitalhustle.certis.features.goal.model.GoalPage
import ru.digitalhustle.certis.features.goal.model.GoalPlanPreview
import ru.digitalhustle.certis.features.goal.model.GoalPlanPreviewData
import ru.digitalhustle.certis.features.goal.model.GoalView
import java.util.UUID

interface GoalReadService {

    fun getPage(userId: UUID, filter: GoalFilter): GoalPage

    fun getOverview(userId: UUID, filter: GoalOverviewFilter): GoalOverview

    fun preview(data: GoalPlanPreviewData): GoalPlanPreview

    fun getById(id: UUID, userId: UUID): GoalView

    fun getContributions(goalId: UUID, userId: UUID, filter: GoalContributionFilter): GoalContributionPage
}
