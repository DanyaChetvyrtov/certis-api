package ru.digitalhustle.certis.features.category.command.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.category.command.model.UpdateCategoryData
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.category.model.Category
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class CategoryRepository(
    private val dsl: DSLContext,
) {

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Category? =
        dsl.selectFrom(Tables.CATEGORIES)
            .where(
                Tables.CATEGORIES.ID.eq(id)
                    .and(Tables.CATEGORIES.USER_ID.eq(userId)),
            )
            .fetchOneInto(Category::class.java)

    fun findByIdAndUserIdForShare(
        id: UUID,
        userId: UUID,
    ): Category? =
        dsl.selectFrom(Tables.CATEGORIES)
            .where(
                Tables.CATEGORIES.ID.eq(id)
                    .and(Tables.CATEGORIES.USER_ID.eq(userId)),
            )
            .forShare()
            .fetchOneInto(Category::class.java)

    fun findAllByIdsAndUserIdForShare(
        ids: Collection<UUID>,
        userId: UUID,
    ): List<Category> =
        dsl.selectFrom(Tables.CATEGORIES)
            .where(
                Tables.CATEGORIES.ID.`in`(ids)
                    .and(Tables.CATEGORIES.USER_ID.eq(userId)),
            )
            .orderBy(Tables.CATEGORIES.ID)
            .forShare()
            .fetchInto(Category::class.java)

    fun findByIdAndUserIdForUpdate(
        id: UUID,
        userId: UUID,
    ): Category? =
        dsl.selectFrom(Tables.CATEGORIES)
            .where(
                Tables.CATEGORIES.ID.eq(id)
                    .and(Tables.CATEGORIES.USER_ID.eq(userId)),
            )
            .forUpdate()
            .fetchOneInto(Category::class.java)

    fun insert(category: Category): Category =
        dsl.insertInto(Tables.CATEGORIES)
            .set(dsl.newRecord(Tables.CATEGORIES, category))
            .returning()
            .fetchSingleInto(Category::class.java)

    fun insertAll(categories: Collection<Category>) {
        if (categories.isEmpty()) {
            return
        }

        dsl.batchInsert(
            categories.map { category ->
                dsl.newRecord(Tables.CATEGORIES, category)
            },
        ).execute()
    }

    fun updateActive(category: UpdateCategoryData): Category? =
        dsl.update(Tables.CATEGORIES)
            .set(Tables.CATEGORIES.NAME, category.name)
            .set(Tables.CATEGORIES.ICON, category.icon)
            .set(Tables.CATEGORIES.COLOR, category.color)
            .where(
                Tables.CATEGORIES.ID.eq(category.id)
                    .and(Tables.CATEGORIES.USER_ID.eq(category.userId))
                    .and(Tables.CATEGORIES.ARCHIVED_AT.isNull()),
            )
            .returning()
            .fetchOneInto(Category::class.java)

    fun archive(
        id: UUID,
        userId: UUID,
        archivedAt: OffsetDateTime,
    ): Boolean =
        dsl.update(Tables.CATEGORIES)
            .set(Tables.CATEGORIES.ARCHIVED_AT, archivedAt)
            .where(
                Tables.CATEGORIES.ID.eq(id)
                    .and(Tables.CATEGORIES.USER_ID.eq(userId))
                    .and(Tables.CATEGORIES.ARCHIVED_AT.isNull()),
            )
            .execute() > 0

    fun restore(
        id: UUID,
        userId: UUID,
    ): Boolean =
        dsl.update(Tables.CATEGORIES)
            .setNull(Tables.CATEGORIES.ARCHIVED_AT)
            .where(
                Tables.CATEGORIES.ID.eq(id)
                    .and(Tables.CATEGORIES.USER_ID.eq(userId))
                    .and(Tables.CATEGORIES.ARCHIVED_AT.isNotNull),
            )
            .execute() > 0

    fun countActiveExpenseCategories(
        userId: UUID,
        categoryIds: Collection<UUID>,
    ): Int {
        if (categoryIds.isEmpty()) {
            return 0
        }

        return dsl.fetchCount(
            Tables.CATEGORIES,
            Tables.CATEGORIES.USER_ID.eq(userId)
                .and(Tables.CATEGORIES.ID.`in`(categoryIds))
                .and(Tables.CATEGORIES.TYPE.eq(CategoryType.EXPENSE.name))
                .and(Tables.CATEGORIES.ARCHIVED_AT.isNull()),
        )
    }
}
