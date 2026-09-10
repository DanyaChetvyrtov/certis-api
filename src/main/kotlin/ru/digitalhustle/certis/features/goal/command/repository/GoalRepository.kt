package ru.digitalhustle.certis.features.goal.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.goal.model.Goal
import java.util.UUID

@Repository
class GoalRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Goal? =
        dsl.selectFrom(Tables.GOALS)
            .where(
                Tables.GOALS.ID.eq(id)
                    .and(Tables.GOALS.USER_ID.eq(userId)),
            )
            .fetchOneInto(Goal::class.java)

    fun findByIdAndUserIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Goal? =
        dsl.selectFrom(Tables.GOALS)
            .where(
                Tables.GOALS.ID.eq(id)
                    .and(Tables.GOALS.USER_ID.eq(userId)),
            )
            .forUpdate()
            .fetchOneInto(Goal::class.java)

    fun insert(goal: Goal): Goal =
        dsl.insertInto(Tables.GOALS)
            .set(dsl.newRecord(Tables.GOALS, goal))
            .returning()
            .fetchSingleInto(Goal::class.java)

    fun update(goal: Goal): Goal =
        dsl.update(Tables.GOALS)
            .set(Tables.GOALS.NAME, goal.name)
            .set(Tables.GOALS.TARGET_AMOUNT, goal.targetAmount)
            .set(Tables.GOALS.DEADLINE, goal.deadline)
            .set(Tables.GOALS.CONTRIBUTION_PLAN_TYPE, goal.contributionPlanType.name)
            .set(Tables.GOALS.MONTHLY_CONTRIBUTION_AMOUNT, goal.monthlyContributionAmount)
            .set(Tables.GOALS.ICON, goal.icon)
            .set(Tables.GOALS.COLOR, goal.color)
            .set(Tables.GOALS.STATUS, goal.status.name)
            .set(Tables.GOALS.UPDATED_AT, goal.updatedAt)
            .set(Tables.GOALS.ACHIEVED_AT, goal.achievedAt)
            .set(Tables.GOALS.ARCHIVED_AT, goal.archivedAt)
            .where(
                Tables.GOALS.ID.eq(goal.id)
                    .and(Tables.GOALS.USER_ID.eq(goal.userId)),
            )
            .returning()
            .fetchSingleInto(Goal::class.java)
}
