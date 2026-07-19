package ru.digitalhustle.certis.features.goal.command.util

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.features.goal.command.service.GoalService
import ru.digitalhustle.certis.features.goal.enums.GoalStatus
import ru.digitalhustle.certis.features.goal.model.Goal
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.math.BigDecimal
import java.util.UUID

@Component
class GoalLifecycleManager(
    private val goalService: GoalService,
    private val applicationClock: ApplicationClock,
) {

    fun achieveIfFunded(goal: Goal, savedAmount: BigDecimal) {
        if (savedAmount >= goal.targetAmount) {
            val now = applicationClock.now()
            goalService.update(
                goal.copy(
                    status = GoalStatus.ACHIEVED,
                    achievedAt = now,
                    archivedAt = now,
                ),
            )
        }
    }

    fun reopenIfUnderfunded(goal: Goal, savedAmount: BigDecimal) {
        if (goal.status == GoalStatus.ACHIEVED && savedAmount < goal.targetAmount) {
            goalService.update(
                goal.copy(
                    status = GoalStatus.ACTIVE,
                    achievedAt = null,
                    archivedAt = null,
                ),
            )
        }
    }

    fun cancel(
        goalId: UUID,
        userId: UUID,
    ) {
        val goal = goalService.getByIdForUpdate(goalId, userId)

        if (goal.status == GoalStatus.CANCELLED) {
            return
        }

        goalService.update(
            goal.copy(
                status = GoalStatus.CANCELLED,
                achievedAt = null,
                archivedAt = applicationClock.now(),
            ),
        )
    }
}
