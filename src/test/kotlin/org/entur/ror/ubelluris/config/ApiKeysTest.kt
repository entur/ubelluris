package org.entur.ror.ubelluris.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ApiKeysTest {

    @Test
    fun shouldUseApiKeyValuesFromConstructor() {
        val apiKeys = ApiKeys(
            stopsDataApiKey = "stopsDataApiKey",
            timetableDataApiKey = "timetableDataApiKey"
        )

        assertThat(apiKeys.stopsDataApiKey).isEqualTo("stopsDataApiKey")
        assertThat(apiKeys.timetableDataApiKey).isEqualTo("timetableDataApiKey")
    }
}