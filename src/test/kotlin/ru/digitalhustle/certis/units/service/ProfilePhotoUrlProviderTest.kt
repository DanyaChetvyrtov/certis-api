package ru.digitalhustle.certis.units.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.digitalhustle.certis.config.properties.AppApiProperties
import ru.digitalhustle.certis.service.profile.ProfilePhotoUrlProvider
import java.util.UUID

class ProfilePhotoUrlProviderTest {

    @Test
    fun `should create public profile photo url`() {
        // given
        val profileId = UUID.randomUUID()
        val provider = ProfilePhotoUrlProvider(
            AppApiProperties(publicUrl = "https://api.certis.test/"),
        )

        // when
        val url = provider.get(profileId)

        // then
        assertThat(url)
            .isEqualTo("https://api.certis.test/api/v1/profiles/$profileId/photo")
    }
}
