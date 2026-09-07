package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.enums.GoalStatus
import ru.digitalhustle.certis.exception.custom.NotFoundException
import ru.digitalhustle.certis.model.entity.Goal
import ru.digitalhustle.certis.model.goal.NewGoal
import ru.digitalhustle.certis.repository.GoalRepository
import ru.digitalhustle.certis.service.domain.GoalService
import ru.digitalhustle.certis.time.ApplicationClock
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
