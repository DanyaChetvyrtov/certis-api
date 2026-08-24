package ru.digitalhustle.certis.provider

import org.springframework.stereotype.Component
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.model.NewCategory
import java.util.UUID

@Component
class DefaultCategoryProvider {

    fun getByUserId(userId: UUID): List<NewCategory> =
        DEFAULT_CATEGORIES.map { category ->
            NewCategory(
                userId = userId,
                name = category.name,
                type = category.type,
                icon = category.icon,
                color = category.color,
            )
        }

    private data class DefaultCategory(
        val name: String,
        val type: CategoryType,
        val icon: String,
        val color: String,
    )

    private companion object {
        private val DEFAULT_CATEGORIES = listOf(
            DefaultCategory("Food", CategoryType.EXPENSE, "utensils", "#E58E4E"),
            DefaultCategory("Transport", CategoryType.EXPENSE, "transport", "#5982B3"),
            DefaultCategory("Housing", CategoryType.EXPENSE, "home", "#8969AD"),
            DefaultCategory("Utilities", CategoryType.EXPENSE, "repeat", "#429792"),
            DefaultCategory("Health", CategoryType.EXPENSE, "heart", "#E6655A"),
            DefaultCategory("Entertainment", CategoryType.EXPENSE, "gift", "#BC9555"),
            DefaultCategory("Other", CategoryType.EXPENSE, "shopping-cart", "#8C9AB8"),
            DefaultCategory("Salary", CategoryType.INCOME, "briefcase", "#10B981"),
            DefaultCategory("Bonus", CategoryType.INCOME, "gift", "#BC9555"),
            DefaultCategory("Investment", CategoryType.INCOME, "briefcase", "#5982B3"),
            DefaultCategory("Other", CategoryType.INCOME, "gift", "#8C9AB8"),
        )
    }
}
