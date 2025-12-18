package org.entur.ror.ubelluris.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CliConfigTest {

    @Test
    fun config() {
        val config = CliConfig(
            stopsDataUrl = "stopsUrl",
            stopsDataApiKey = "stopsApiKey",
            timetableDataUrl = "timetableDataUrl",
            timetableDataApiKey = "timetableDataApiKey",
            timetableProviders = listOf("foo1", "foo2", "foo3")
        )
        assertThat(config.stopsDataUrl).isEqualTo("stopsUrl")
        assertThat(config.stopsDataApiKey).isEqualTo("stopsApiKey")
        assertThat(config.timetableDataUrl).isEqualTo("timetableDataUrl")
        assertThat(config.timetableDataApiKey).isEqualTo("timetableDataApiKey")
        assertThat(config.timetableProviders)
            .hasSize(3)
            .containsExactly("foo1", "foo2", "foo3")
    }

}