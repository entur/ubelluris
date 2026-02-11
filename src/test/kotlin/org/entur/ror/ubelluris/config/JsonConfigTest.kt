package org.entur.ror.ubelluris.config

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.TransportMode
import org.junit.jupiter.api.Test

class JsonConfigTest {

    @Test
    fun shouldDeserializeCliConfigFromJson() {
        val json = """
        {
          "sourceCodespace": "SE:050",
          "targetCodespace": "SAM",
          "illegalPublicCodes": ["*", "-"]
        }
        """.trimIndent()

        val config = JsonConfig.loadCliConfig(json.byteInputStream())

        assertThat(config.sourceCodespace).isEqualTo("SE:050")
        assertThat(config.targetCodespace).isEqualTo("SAM")
        assertThat(config.timetableProviders).containsExactly("vt", "halland", "skane")
        assertThat(config.transportModes).containsExactly(TransportMode.TRAM, TransportMode.WATER)
        assertThat(config.illegalPublicCodes).containsExactly("*", "-")
    }

    @Test
    fun shouldDeserializeCliConfigWithOverrides() {
        val json = """
        {
          "sourceCodespace": "SE:050",
          "targetCodespace": "SAM",
          "timetableProviders": ["provider1"],
          "transportModes": ["tram"]
        }
        """.trimIndent()

        val config = JsonConfig.loadCliConfig(json.byteInputStream())

        assertThat(config.sourceCodespace).isEqualTo("SE:050")
        assertThat(config.targetCodespace).isEqualTo("SAM")
        assertThat(config.timetableProviders).containsExactly("provider1")
        assertThat(config.transportModes).containsExactly(TransportMode.TRAM)
    }

}
