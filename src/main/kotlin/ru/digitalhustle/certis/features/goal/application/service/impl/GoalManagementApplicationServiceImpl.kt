package ru.digitalhustle.certis.features.goal.application.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.goal.application.service.GoalContributionSupport
import ru.digitalhustle.certis.features.goal.application.service.GoalManagementApplicationService
import ru.digitalhustle.certis.features.goal.command.service.GoalService
import ru.digitalhustle.certis.features.goal.command.util.GoalLifecycleManager
import ru.digitalhustle.certis.features.goal.command.util.GoalPlanner
import ru.digitalhustle.certis.features.goal.model.CreateGoalData
import ru.digitalhustle.certis.features.goal.model.GoalView
import ru.digitalhustle.certis.features.goal.model.UpdateGoalData
import ru.digitalhustle.certis.features.goal.query.service.GoalQueryService
import java.util.UUID

@Service
class GoalManagementApplicationServiceImpl(
    private val goalService: GoalService,
    private val goalQueryService: GoalQueryService,
    private val goalContributionSupport: GoalContributionSupport,
    private val goalPlanner: GoalPlanner,
    private val goalLifecycleManager: GoalLifecycleManager,
) : GoalManagementApplicationService {

    @Transactional
    override fun create(data: CreateGoalData): GoalView {
        val goal = goalService.save(goalPlanner.create(data))

        data.initialContribution?.let { contribution ->
            goalContributionSupport.saveInitial(goal, contribution)
        }

        return goalQueryService.getById(goal.id, data.userId)
    }

    @Transactional
    override fun update(data: UpdateGoalData): GoalView {
        val current = goalService.getByIdForUpdate(data.id, data.userId)
        val savedAmount = goalContributionSupport.getSavedAmount(data.id, data.userId)

        goalService.update(goalPlanner.update(current, data, savedAmount))

        return goalQueryService.getById(data.id, data.userId)
    }

    @Transactional
    override fun cancel(
        goalId: UUID,
        userId: UUID,
    ) {
        goalLifecycleManager.cancel(goalId, userId)
    }
}
