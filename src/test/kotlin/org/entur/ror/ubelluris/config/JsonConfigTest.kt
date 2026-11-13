package org.entur.ror.ubelluris.config

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.entur.ror.ubelluris.model.TransportMode
import org.junit.jupiter.api.Test

class JsonConfigTest {
    @Test
    fun shouldDeserializeCliConfigFromJson() {
        val json =
            """
            {
              "sourceCodespace": "SE:050",
              "targetCodespace": "SAM",
              "illegalPublicCodes": ["-"]
              "filterPublicCodes": ["*"]
            }
            """.trimIndent()

        val config = JsonConfig.loadCliConfig(json.byteInputStream())

        assertThat(config.sourceCodespace).isEqualTo("SE:050")
        assertThat(config.targetCodespace).isEqualTo("SAM")
        assertThat(config.timetableProviders).containsExactly("vt", "halland", "skane", "sj")
        assertThat(config.transportModes).containsExactly(TransportMode.TRAM, TransportMode.WATER)
        assertThat(config.illegalPublicCodes).containsExactly("-")
        assertThat(config.filterPublicCodes).containsExactly("*")
        assertThat(config.lineTransportModesToRemove).containsExactly(TransportMode.TAXI)
    }

    @Test
    fun shouldDeserializeCliConfigWithOverrides() {
        val json =
            """
            {
              "sourceCodespace": "SE:050",
              "targetCodespace": "SAM",
              "timetableProviders": ["provider1", "sj"],
              "transportModes": ["tram"],
              "lineTransportModesToRemove": ["taxi", "water"]
            }
            """.trimIndent()

        val config = JsonConfig.loadCliConfig(json.byteInputStream())

        assertThat(config.sourceCodespace).isEqualTo("SE:050")
        assertThat(config.targetCodespace).isEqualTo("SAM")
        assertThat(config.timetableProviders).containsExactly("provider1", "sj")
        assertThat(config.transportModes).containsExactly(TransportMode.TRAM)
        assertThat(config.lineTransportModesToRemove).containsExactly(TransportMode.TAXI, TransportMode.WATER)
    }

    @Test
    fun shouldDeserializeSalesPatchesAndGroupCarriersByProvider() {
        val config =
            JsonConfig.loadCliConfig(
                configWithSalesPatches("""[{"provider": "sj", "carrier": "74"}, {"provider": "sj", "carrier": "300"}]""")
                    .byteInputStream(),
            )

        assertThat(config.salesPatches).containsExactly(SalesPatch("sj", "74"), SalesPatch("sj", "300"))
        assertThat(config.salesPatchCarriers("sj")).containsExactlyInAnyOrder("74", "300")
        assertThat(config.salesPatchCarriers("vt")).isEmpty()
    }

    @Test
    fun shouldFailWhenSalesPatchProviderIsNotATimetableProvider() {
        val json = configWithSalesPatches("""[{"provider": "unknown", "carrier": "74"}]""")

        assertThatThrownBy { JsonConfig.loadCliConfig(json.byteInputStream()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("[unknown]")
    }

    @Test
    fun shouldCompareSalesPatchProviderCaseSensitively() {
        val json = configWithSalesPatches("""[{"provider": "SJ", "carrier": "74"}]""")

        assertThatThrownBy { JsonConfig.loadCliConfig(json.byteInputStream()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("[SJ]")
    }

    @Test
    fun shouldFailWhenSalesDataIsUsedWithoutSalesPatches() {
        val json = configWithSalesPatches("[]", useSalesData = true)

        assertThatThrownBy { JsonConfig.loadCliConfig(json.byteInputStream()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("no salesPatches")
    }

    private fun configWithSalesPatches(
        salesPatches: String,
        useSalesData: Boolean = false,
    ) = """
        {
          "sourceCodespace": "SE:050",
          "targetCodespace": "SAM",
          "timetableProviders": ["sj", "vt"],
          "salesPatches": $salesPatches,
          "timetableFilterFeatures": { "useSalesData": $useSalesData }
        }
        """.trimIndent()
}
