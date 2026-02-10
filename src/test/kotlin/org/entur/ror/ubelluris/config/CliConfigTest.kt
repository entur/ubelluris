package org.entur.ror.ubelluris.config

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.TransportMode
import org.junit.jupiter.api.Test

class CliConfigTest {

    @Test
    fun config() {
        val config = CliConfig(
            sourceCodespace = "sourceCodespace",
            targetCodespace = "targetCodespace",
            timetableProviders = listOf("foo1", "foo2", "foo3"),
            transportModes = listOf(TransportMode.TRAM, TransportMode.WATER),
        )
        assertThat(config.sourceCodespace).isEqualTo("sourceCodespace")
        assertThat(config.targetCodespace).isEqualTo("targetCodespace")
        assertThat(config.timetableProviders)
            .containsExactly("foo1", "foo2", "foo3")
        assertThat(config.transportModes)
            .containsExactly(TransportMode.TRAM, TransportMode.WATER)
    }

    @Test
    fun shouldUseDefaults() {
        val config = CliConfig(
            sourceCodespace = "SE:050",
            targetCodespace = "SAM",
        )
        assertThat(config.timetableProviders)
            .containsExactly("vt", "halland", "skane")
        assertThat(config.transportModes)
            .containsExactly(TransportMode.TRAM, TransportMode.WATER)
        assertThat(config.illegalPublicCodes)
            .containsExactly("*", "-")
    }

}
