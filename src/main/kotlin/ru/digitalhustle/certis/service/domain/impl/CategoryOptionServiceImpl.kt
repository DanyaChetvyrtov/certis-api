package ru.digitalhustle.certis.service.domain.impl

import org.springframework.stereotype.Service
import ru.digitalhustle.certis.enums.CategoryType
import ru.digitalhustle.certis.model.category.CategoryOption
import ru.digitalhustle.certis.repository.CategoryOptionRepository
import ru.digitalhustle.certis.service.domain.CategoryOptionService
import java.util.UUID

@Service
class CategoryOptionServiceImpl(
    private val categoryOptionRepository: CategoryOptionRepository,
) : CategoryOptionService {

    override fun getOptions(
        userId: UUID,
        type: CategoryType,
    ): List<CategoryOption> = categoryOptionRepository.findActiveByUserIdAndType(userId, type)
}
