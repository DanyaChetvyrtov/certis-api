package ru.digitalhustle.certis.repository

import org.jooq.DSLContext
import org.jooq.Records
import org.jooq.generated.Tables
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.model.category.CategoryOption
import java.util.UUID

@Repository
class CategoryOptionRepository(
    private val dsl: DSLContext,
) {

    fun findActiveByUserIdAndType(
        userId: UUID,
        type: CategoryType,
    ): List<CategoryOption> =
        dsl.select(
            Tables.CATEGORIES.ID,
            Tables.CATEGORIES.NAME,
            Tables.CATEGORIES.ICON,
            Tables.CATEGORIES.COLOR,
        )
            .from(Tables.CATEGORIES)
            .where(
                Tables.CATEGORIES.USER_ID.eq(userId)
                    .and(Tables.CATEGORIES.TYPE.eq(type.name))
                    .and(Tables.CATEGORIES.ARCHIVED_AT.isNull()),
            )
            .orderBy(
                DSL.lower(Tables.CATEGORIES.NAME).asc(),
                Tables.CATEGORIES.ID.asc(),
            )
            .fetch(Records.mapping(::CategoryOption))
}
