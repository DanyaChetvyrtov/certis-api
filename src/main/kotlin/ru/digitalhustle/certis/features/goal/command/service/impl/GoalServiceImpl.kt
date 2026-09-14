package ru.digitalhustle.certis.features.goal.command.service.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.features.goal.command.repository.GoalRepository
import ru.digitalhustle.certis.features.goal.command.service.GoalService
import ru.digitalhustle.certis.features.goal.enums.GoalStatus
import ru.digitalhustle.certis.features.goal.model.Goal
import ru.digitalhustle.certis.features.goal.model.NewGoal
import ru.digitalhustle.certis.util.time.ApplicationClock
import java.util.UUID

@Service
class GoalServiceImpl(
    private val goalRepository: GoalRepository,
    private val applicationClock: ApplicationClock,
) : GoalService {

    override fun getById(
        id: UUID,
        userId: UUID,
    ): Goal = goalRepository.findByIdAndUserId(id, userId) ?: throw NotFoundException.entity("Goal")

    override fun getByIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Goal = goalRepository.findByIdAndUserIdForUpdate(id, userId) ?: throw NotFoundException.entity("Goal")

    override fun save(goal: NewGoal): Goal {
        val now = applicationClock.now()

        return goalRepository.insert(
            Goal(
                id = UUID.randomUUID(),
                userId = goal.userId,
                name = goal.name.trim(),
                targetAmount = goal.targetAmount,
                currency = goal.currency,
                deadline = goal.targetMonth.atEndOfMonth(),
                contributionPlanType = goal.contributionPlanType,
                monthlyContributionAmount = goal.monthlyContributionAmount,
                icon = goal.icon.trim(),
                color = goal.color.uppercase(),
                status = GoalStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
                achievedAt = null,
                archivedAt = null,
            ),
        )
    }

    override fun update(goal: Goal): Goal =
        goalRepository.update(goal.copy(updatedAt = applicationClock.now()))
}
