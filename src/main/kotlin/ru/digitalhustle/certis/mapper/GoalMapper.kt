package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.GoalContributionDto
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
import ru.digitalhustle.certis.model.entity.GoalTransaction
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

@Mapper(config = BaseMapperConfig::class)
interface GoalMapper {

    fun convert(source: GoalFilterRq): GoalFilter

    fun convert(source: GoalOverviewRq): GoalOverviewFilter

    fun convert(source: GoalPlanPreviewRq): GoalPlanPreviewData

    fun convert(source: CreateGoalRq, userId: UUID): CreateGoalData

    fun convert(source: UpdateGoalRq, id: UUID, userId: UUID): UpdateGoalData

    fun convert(
        source: CreateGoalContributionRq,
        goalId: UUID,
        userId: UUID,
        idempotencyKey: String?,
    ): CreateGoalContributionData

    fun convert(source: GoalContributionFilterRq): GoalContributionFilter

    fun convert(source: GoalView): GoalDto

    fun convert(source: GoalPage): GoalPageRs

    fun convert(source: GoalPlanPreview): GoalPlanPreviewRs

    fun convert(source: GoalOverview): GoalOverviewRs

    fun convert(source: GoalContributionResult): GoalContributionRs

    fun convert(source: GoalContributionPage): GoalContributionPageRs

    fun convert(source: GoalProgress): GoalProgressRs

    @Mapping(target = "reversalOfContributionId", source = "reversalOfGoalTransactionId")
    @Mapping(target = "contributedAt", source = "date")
    fun convert(source: GoalTransaction): GoalContributionDto
}
