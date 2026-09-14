package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import ru.digitalhustle.certis.api.dto.GoalContributionDto
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
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.goal.model.CreateGoalContributionData
import ru.digitalhustle.certis.features.goal.model.CreateGoalData
import ru.digitalhustle.certis.features.goal.model.GoalContributionFilter
import ru.digitalhustle.certis.features.goal.model.GoalContributionPage
import ru.digitalhustle.certis.features.goal.model.GoalContributionResult
import ru.digitalhustle.certis.features.goal.model.GoalFilter
import ru.digitalhustle.certis.features.goal.model.GoalOverview
import ru.digitalhustle.certis.features.goal.model.GoalOverviewFilter
import ru.digitalhustle.certis.features.goal.model.GoalPage
import ru.digitalhustle.certis.features.goal.model.GoalPlanPreview
import ru.digitalhustle.certis.features.goal.model.GoalPlanPreviewData
import ru.digitalhustle.certis.features.goal.model.GoalProgress
import ru.digitalhustle.certis.features.goal.model.GoalTransaction
import ru.digitalhustle.certis.features.goal.model.GoalView
import ru.digitalhustle.certis.features.goal.model.UpdateGoalData
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
