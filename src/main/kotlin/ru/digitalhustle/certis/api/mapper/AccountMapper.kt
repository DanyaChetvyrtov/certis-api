package ru.digitalhustle.certis.api.mapper

import org.mapstruct.Mapper
import ru.digitalhustle.certis.api.dto.AccountDto
import ru.digitalhustle.certis.api.dto.request.CreateAccountRq
import ru.digitalhustle.certis.api.dto.request.UpdateAccountRq
import ru.digitalhustle.certis.config.BaseMapperConfig
import ru.digitalhustle.certis.features.account.command.model.NewAccount
import ru.digitalhustle.certis.features.account.command.model.UpdateAccountData
import ru.digitalhustle.certis.features.account.model.AccountPreview
import java.util.UUID

@Mapper(config = BaseMapperConfig::class)
interface AccountMapper {

    fun convert(source: CreateAccountRq, userId: UUID): NewAccount

    fun convert(source: UpdateAccountRq, id: UUID, userId: UUID): UpdateAccountData

    fun convert(source: AccountPreview): AccountDto
}
