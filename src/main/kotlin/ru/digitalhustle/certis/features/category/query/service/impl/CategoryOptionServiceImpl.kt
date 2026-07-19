package ru.digitalhustle.certis.features.category.query.service.impl

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhustle.certis.features.category.enums.CategoryType
import ru.digitalhustle.certis.features.category.query.model.CategoryOption
import ru.digitalhustle.certis.features.category.query.repository.CategoryOptionQueryRepository
import ru.digitalhustle.certis.features.category.query.service.CategoryOptionService
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CategoryOptionServiceImpl(
    private val categoryOptionRepository: CategoryOptionQueryRepository,
) : CategoryOptionService {

    override fun getOptions(
        userId: UUID,
        type: CategoryType,
    ): List<CategoryOption> = categoryOptionRepository.findActiveByUserIdAndType(userId, type)
}
