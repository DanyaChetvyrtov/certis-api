package ru.digitalhustle.certis.api.controller.impl

import org.springframework.web.bind.annotation.RestController
import ru.digitalhustle.certis.api.controller.GoalController
import ru.digitalhustle.certis.api.dto.GoalDto
import ru.digitalhustle.certis.api.dto.request.CreateGoalContributionRq
import ru.digitalhustle.certis.api.dto.request.CreateGoalRq
import ru.digitalhustle.certis.api.dto.request.GoalContributionFilterRq
import ru.digitalhustle.certis.api.dto.request.GoalFilterRq
import ru.digitalhustle.certis.api.dto.request.GoalOverviewRq
import ru.digitalhustle.certis.api.dto.request.GoalPlanPreviewRq
import ru.digitalhustle.certis.api.dto.request.UpdateGoalRq
import ru.digitalhustle.certis.api.dto.response.GoalContributionPageRs
import ru.digitalhustle.certis.api.dto.response.GoalContributionRs
import ru.digitalhustle.certis.api.dto.response.GoalOverviewRs
import ru.digitalhustle.certis.api.dto.response.GoalPageRs
import ru.digitalhustle.certis.api.dto.response.GoalPlanPreviewRs
import ru.digitalhustle.certis.api.dto.response.GoalProgressRs
import ru.digitalhustle.certis.api.mapper.GoalMapper
import ru.digitalhustle.certis.features.goal.application.service.GoalContributionApplicationService
import ru.digitalhustle.certis.features.goal.application.service.GoalManagementApplicationService
import ru.digitalhustle.certis.features.goal.query.service.GoalReadService
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@RestController
class GoalControllerImpl(
    private val goalReadService: GoalReadService,
    private val goalManagementService: GoalManagementApplicationService,
    private val goalContributionService: GoalContributionApplicationService,
    private val goalMapper: GoalMapper,
) : GoalController {

    override fun getGoals(
        filterRq: GoalFilterRq,
        jwtDetails: JwtDetails,

    ): GoalPageRs = goalMapper.convert(goalReadService.getPage(jwtDetails.id, goalMapper.convert(filterRq)))

    override fun getOverview(
        overviewRq: GoalOverviewRq,
        jwtDetails: JwtDetails,
    ): GoalOverviewRs = goalMapper.convert(goalReadService.getOverview(jwtDetails.id, goalMapper.convert(overviewRq)))

    override fun previewPlan(previewRq: GoalPlanPreviewRq): GoalPlanPreviewRs =
        goalMapper.convert(goalReadService.preview(goalMapper.convert(previewRq)))

    override fun createGoal(
        createGoalRq: CreateGoalRq,
        jwtDetails: JwtDetails,
    ): GoalDto = goalMapper.convert(goalManagementService.create(goalMapper.convert(createGoalRq, jwtDetails.id)))

    override fun getGoal(
        goalId: UUID,
        jwtDetails: JwtDetails,
    ): GoalDto = goalMapper.convert(goalReadService.getById(goalId, jwtDetails.id))

    override fun updateGoal(
        goalId: UUID,
        updateGoalRq: UpdateGoalRq,
        jwtDetails: JwtDetails,
    ): GoalDto = goalMapper.convert(
        goalManagementService.update(goalMapper.convert(updateGoalRq, goalId, jwtDetails.id)),
    )

    override fun addContribution(
        goalId: UUID,
        idempotencyKey: String?,
        contributionRq: CreateGoalContributionRq,
        jwtDetails: JwtDetails,
    ): GoalContributionRs = goalMapper.convert(
        goalContributionService.contribute(
            goalMapper.convert(contributionRq, goalId, jwtDetails.id, idempotencyKey),
        ),
    )

    override fun getContributions(
        goalId: UUID,
        filterRq: GoalContributionFilterRq,
        jwtDetails: JwtDetails,
    ): GoalContributionPageRs = goalMapper.convert(
        goalReadService.getContributions(goalId, jwtDetails.id, goalMapper.convert(filterRq)),
    )

    override fun refundContribution(
        goalId: UUID,
        contributionId: UUID,
        jwtDetails: JwtDetails,
    ): GoalProgressRs = goalMapper.convert(
        goalContributionService.refundContribution(goalId, contributionId, jwtDetails.id),
    )

    override fun cancelGoal(
        goalId: UUID,
        jwtDetails: JwtDetails,
    ): Unit = goalManagementService.cancel(goalId, jwtDetails.id)
}
