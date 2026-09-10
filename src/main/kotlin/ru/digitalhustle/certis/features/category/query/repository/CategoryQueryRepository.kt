package ru.digitalhustle.certis.features.category.query.repository

import org.jooq.DSLContext
import org.jooq.generated.Tables
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.features.category.model.Category
import java.util.UUID

@Repository
class CategoryQueryRepository(
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

    fun findAllByUserId(userId: UUID): List<Category> =
        dsl.selectFrom(Tables.CATEGORIES)
            .where(Tables.CATEGORIES.USER_ID.eq(userId))
            .orderBy(
                Tables.CATEGORIES.ARCHIVED_AT.asc().nullsFirst(),
                Tables.CATEGORIES.TYPE.asc(),
                Tables.CATEGORIES.NAME.asc(),
            )
            .fetchInto(Category::class.java)
}
