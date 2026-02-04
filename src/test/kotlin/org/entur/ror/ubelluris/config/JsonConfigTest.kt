package org.entur.ror.ubelluris.config

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.TransportMode
import org.junit.jupiter.api.Test

class JsonConfigTest {

    @Test
    fun shouldDeserializeCliConfigFromJson() {
        val json = """
        {
          "stopsDataUrl": "https://example.com/stops",
          "timetableDataUrl": "https://example.com/timetable",
          "sourceCodespace": "SE:050",
          "targetCodespace": "SAM",
          "timetableProviders": ["provider1"],
          "transportModes": ["tram", "water"]
        }
        """.trimIndent()

        val config = JsonConfig.loadCliConfig(json.byteInputStream())

        assertThat(config.stopsDataUrl).isEqualTo("https://example.com/stops")
        assertThat(config.timetableDataUrl).isEqualTo("https://example.com/timetable")
        assertThat(config.sourceCodespace).isEqualTo("SE:050")
        assertThat(config.targetCodespace).isEqualTo("SAM")
        assertThat(config.timetableProviders).containsExactly("provider1")
        assertThat(config.transportModes).containsExactly(TransportMode.TRAM, TransportMode.WATER)
    }

}