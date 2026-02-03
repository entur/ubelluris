package org.entur.ror.ubelluris.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CliConfigTest {

    @Test
    fun config() {
        val config = CliConfig(
            stopsDataUrl = "stopsUrl",
            timetableDataUrl = "timetableDataUrl",
            sourceCodespace = "sourceCodespace",
            targetCodespace = "targetCodespace",
            timetableProviders = listOf("foo1", "foo2", "foo3")
        )
        assertThat(config.stopsDataUrl).isEqualTo("stopsUrl")
        assertThat(config.timetableDataUrl).isEqualTo("timetableDataUrl")
        assertThat(config.sourceCodespace).isEqualTo("sourceCodespace")
        assertThat(config.targetCodespace).isEqualTo("targetCodespace")
        assertThat(config.timetableProviders)
            .hasSize(3)
            .containsExactly("foo1", "foo2", "foo3")
    }

}