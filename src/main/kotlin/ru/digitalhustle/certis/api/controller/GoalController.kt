package ru.digitalhustle.certis.api.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
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
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.features.security.model.JwtDetails
import java.util.UUID

@Validated
@RequestMapping(PathConstants.GOALS)
interface GoalController {

    @GetMapping
    fun getGoals(
        @ModelAttribute @Valid filterRq: GoalFilterRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): GoalPageRs

    @GetMapping(PathConstants.GOAL_OVERVIEW)
    fun getOverview(
        @ModelAttribute @Valid overviewRq: GoalOverviewRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): GoalOverviewRs

    @PostMapping(PathConstants.GOAL_PLAN_PREVIEW)
    fun previewPlan(
        @RequestBody @Valid previewRq: GoalPlanPreviewRq,
    ): GoalPlanPreviewRs

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createGoal(
        @RequestBody @Valid createGoalRq: CreateGoalRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): GoalDto

    @GetMapping(PathConstants.GOAL_ID)
    fun getGoal(
        @PathVariable goalId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): GoalDto

    @PatchMapping(PathConstants.GOAL_ID)
    fun updateGoal(
        @PathVariable goalId: UUID,
        @RequestBody @Valid updateGoalRq: UpdateGoalRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): GoalDto

    @PostMapping(PathConstants.GOAL_CONTRIBUTIONS)
    @ResponseStatus(HttpStatus.CREATED)
    fun addContribution(
        @PathVariable goalId: UUID,
        @RequestHeader(name = "Idempotency-Key", required = false)
        @Size(min = 1, max = 100)
        idempotencyKey: String?,
        @RequestBody @Valid contributionRq: CreateGoalContributionRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): GoalContributionRs

    @GetMapping(PathConstants.GOAL_CONTRIBUTIONS)
    fun getContributions(
        @PathVariable goalId: UUID,
        @ModelAttribute @Valid filterRq: GoalContributionFilterRq,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): GoalContributionPageRs

    @DeleteMapping(PathConstants.GOAL_CONTRIBUTION_ID)
    fun refundContribution(
        @PathVariable goalId: UUID,
        @PathVariable contributionId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    ): GoalProgressRs

    @DeleteMapping(PathConstants.GOAL_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancelGoal(
        @PathVariable goalId: UUID,
        @AuthenticationPrincipal jwtDetails: JwtDetails,
    )
}
