package org.entur.ror.ubelluris.sax.plugins

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.NetexTypes
import org.junit.jupiter.api.Test
import org.xml.sax.helpers.AttributesImpl

class JourneyPatternStopPointCollectorPluginTest {
    private val plugin = JourneyPatternStopPointCollectorPlugin()

    @Test
    fun shouldFindFirstAndLastStopPointPerJourneyPattern() {
        journeyPattern("JP-1", listOf("JP-1-1" to "1", "JP-1-2" to "2", "JP-1-3" to "3"))
        journeyPattern("JP-2", listOf("JP-2-1" to "1", "JP-2-2" to "2"))

        assertThat(plugin.getFirstStopPointIds()).containsExactlyInAnyOrder("JP-1-1", "JP-2-1")
        assertThat(plugin.getLastStopPointIds()).containsExactlyInAnyOrder("JP-1-3", "JP-2-2")
    }

    @Test
    fun shouldUseOrderAttributeRatherThanDocumentOrder() {
        journeyPattern("JP-1", listOf("JP-1-b" to "2", "JP-1-c" to "3", "JP-1-a" to "1"))

        assertThat(plugin.getFirstStopPointIds()).containsExactly("JP-1-a")
        assertThat(plugin.getLastStopPointIds()).containsExactly("JP-1-c")
    }

    @Test
    fun shouldFallBackToDocumentOrderWhenOrderIsMissing() {
        journeyPattern("JP-1", listOf("JP-1-1" to null, "JP-1-2" to null, "JP-1-3" to null))

        assertThat(plugin.getFirstStopPointIds()).containsExactly("JP-1-1")
        assertThat(plugin.getLastStopPointIds()).containsExactly("JP-1-3")
    }

    @Test
    fun shouldTreatSingleStopPointAsBothFirstAndLast() {
        journeyPattern("JP-1", listOf("JP-1-1" to "1"))

        assertThat(plugin.getFirstStopPointIds()).containsExactly("JP-1-1")
        assertThat(plugin.getLastStopPointIds()).containsExactly("JP-1-1")
    }

    private fun journeyPattern(
        id: String,
        stopPoints: List<Pair<String, String?>>,
    ) {
        plugin.startElement(NetexTypes.JOURNEY_PATTERN, attributes("id" to id), null)
        stopPoints.forEach { (spId, order) ->
            val attrs = if (order != null) attributes("id" to spId, "order" to order) else attributes("id" to spId)
            plugin.startElement(NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN, attrs, null)
            plugin.endElement(NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN, null)
        }
        plugin.endElement(NetexTypes.JOURNEY_PATTERN, null)
    }

    private fun attributes(vararg values: Pair<String, String>) =
        AttributesImpl().apply {
            values.forEach { (name, value) -> addAttribute("", name, name, "CDATA", value) }
        }
}
