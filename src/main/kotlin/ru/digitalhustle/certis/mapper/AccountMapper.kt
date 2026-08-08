package ru.digitalhustle.certis.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.dto.AccountDto
import ru.digitalhustle.certis.dto.request.CreateAccountRq
import ru.digitalhustle.certis.dto.request.UpdateAccountRq
import ru.digitalhustle.certis.model.AccountPreview
import ru.digitalhustle.certis.model.NewAccount
import ru.digitalhustle.certis.model.UpdateAccountData
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface AccountMapper {

    fun convert(source: CreateAccountRq, userId: UUID): NewAccount

    fun convert(source: UpdateAccountRq, id: UUID, userId: UUID): UpdateAccountData

    fun convert(source: AccountPreview): AccountDto
}
