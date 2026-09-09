package ru.digitalhustle.certis.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.controller.GoalController
import ru.digitalhustle.certis.dto.GoalDto
import ru.digitalhustle.certis.dto.request.CreateGoalContributionRq
import ru.digitalhustle.certis.dto.request.CreateGoalRq
import ru.digitalhustle.certis.dto.request.GoalContributionFilterRq
import ru.digitalhustle.certis.dto.request.GoalFilterRq
import ru.digitalhustle.certis.dto.request.GoalOverviewRq
import ru.digitalhustle.certis.dto.request.GoalPlanPreviewRq
import ru.digitalhustle.certis.dto.request.UpdateGoalRq
import ru.digitalhustle.certis.dto.response.GoalContributionPageRs
import ru.digitalhustle.certis.dto.response.GoalContributionRs
import ru.digitalhustle.certis.dto.response.GoalOverviewRs
import ru.digitalhustle.certis.dto.response.GoalPageRs
import ru.digitalhustle.certis.dto.response.GoalPlanPreviewRs
import ru.digitalhustle.certis.dto.response.GoalProgressRs
import ru.digitalhustle.certis.mapper.GoalMapper
import ru.digitalhustle.certis.model.security.JwtDetails
import ru.digitalhustle.certis.service.goal.GoalAggregator
import java.util.UUID

@RestController
class GoalControllerImpl(
    private val goalAggregator: GoalAggregator,
    private val goalMapper: GoalMapper,
) : GoalController {

    override fun getGoals(
        filterRq: GoalFilterRq,
        jwtDetails: JwtDetails,
    ): GoalPageRs = goalMapper.convert(goalAggregator.getPage(jwtDetails.id, goalMapper.convert(filterRq)))

    override fun getOverview(
        overviewRq: GoalOverviewRq,
        jwtDetails: JwtDetails,
    ): GoalOverviewRs = goalMapper.convert(goalAggregator.getOverview(jwtDetails.id, goalMapper.convert(overviewRq)))

    override fun previewPlan(previewRq: GoalPlanPreviewRq): GoalPlanPreviewRs =
        goalMapper.convert(goalAggregator.preview(goalMapper.convert(previewRq)))

    override fun createGoal(
        createGoalRq: CreateGoalRq,
        jwtDetails: JwtDetails,
    ): GoalDto = goalMapper.convert(goalAggregator.create(goalMapper.convert(createGoalRq, jwtDetails.id)))

    override fun getGoal(
        goalId: UUID,
        jwtDetails: JwtDetails,
    ): GoalDto = goalMapper.convert(goalAggregator.getById(goalId, jwtDetails.id))

    override fun updateGoal(
        goalId: UUID,
        updateGoalRq: UpdateGoalRq,
        jwtDetails: JwtDetails,
    ): GoalDto = goalMapper.convert(
        goalAggregator.update(goalMapper.convert(updateGoalRq, goalId, jwtDetails.id)),
    )

    override fun addContribution(
        goalId: UUID,
        idempotencyKey: String?,
        contributionRq: CreateGoalContributionRq,
        jwtDetails: JwtDetails,
    ): GoalContributionRs = goalMapper.convert(
        goalAggregator.contribute(
            goalMapper.convert(contributionRq, goalId, jwtDetails.id, idempotencyKey),
        ),
    )

    override fun getContributions(
        goalId: UUID,
        filterRq: GoalContributionFilterRq,
        jwtDetails: JwtDetails,
    ): GoalContributionPageRs = goalMapper.convert(
        goalAggregator.getContributions(goalId, jwtDetails.id, goalMapper.convert(filterRq)),
    )

    override fun refundContribution(
        goalId: UUID,
        contributionId: UUID,
        jwtDetails: JwtDetails,
    ): GoalProgressRs = goalMapper.convert(
        goalAggregator.refundContribution(goalId, contributionId, jwtDetails.id),
    )

    override fun cancelGoal(
        goalId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = goalAggregator.cancel(goalId, jwtDetails.id)
}
