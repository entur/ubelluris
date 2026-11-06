package org.entur.ror.ubelluris.sax.plugins

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.data.StopPointInJourneyPatternKey
import org.junit.jupiter.api.Test
import org.xml.sax.helpers.AttributesImpl

class JourneyPatternStopPointCollectorPluginTest {
    private val plugin = JourneyPatternStopPointCollectorPlugin()

    @Test
    fun shouldFindFirstAndLastStopPointPerJourneyPattern() {
        journeyPattern("JP-1", listOf("JP-1-1" to "1", "JP-1-2" to "2", "JP-1-3" to "3"))
        journeyPattern("JP-2", listOf("JP-2-1" to "1", "JP-2-2" to "2"))
        plugin.endDocument("doc1.xml")

        assertThat(plugin.getFirstStopPoints()).containsExactlyInAnyOrder(key("JP-1", "JP-1-1", "1"), key("JP-2", "JP-2-1", "1"))
        assertThat(plugin.getLastStopPoints()).containsExactlyInAnyOrder(key("JP-1", "JP-1-3", "3"), key("JP-2", "JP-2-2", "2"))
    }

    @Test
    fun shouldUseOrderAttributeRatherThanDocumentOrder() {
        journeyPattern("JP-1", listOf("JP-1-b" to "2", "JP-1-c" to "3", "JP-1-a" to "1"))
        plugin.endDocument("doc1.xml")

        assertThat(plugin.getFirstStopPoints()).containsExactly(key("JP-1", "JP-1-a", "1"))
        assertThat(plugin.getLastStopPoints()).containsExactly(key("JP-1", "JP-1-c", "3"))
    }

    @Test
    fun shouldIgnoreJourneyPatternWithMissingOrInvalidOrder() {
        journeyPattern("JP-1", listOf("JP-1-1" to "1", "JP-1-2" to null, "JP-1-3" to "3"))
        journeyPattern("JP-2", listOf("JP-2-1" to "1", "JP-2-2" to "x"))
        journeyPattern("JP-3", listOf("JP-3-1" to "1", "JP-3-2" to "2"))
        plugin.endDocument("doc1.xml")

        assertThat(plugin.getFirstStopPoints()).containsExactly(key("JP-3", "JP-3-1", "1"))
        assertThat(plugin.getLastStopPoints()).containsExactly(key("JP-3", "JP-3-2", "2"))
    }

    @Test
    fun shouldTreatSingleStopPointAsBothFirstAndLast() {
        journeyPattern("JP-1", listOf("JP-1-1" to "1"))
        plugin.endDocument("doc1.xml")

        assertThat(plugin.getFirstStopPoints()).containsExactly(key("JP-1", "JP-1-1", "1"))
        assertThat(plugin.getLastStopPoints()).containsExactly(key("JP-1", "JP-1-1", "1"))
    }

    @Test
    fun shouldDistinguishSameStopPointIdInDifferentJourneyPatterns() {
        journeyPattern("JP-1", listOf("SP-A" to "1", "SP-B" to "2"))
        journeyPattern("JP-2", listOf("SP-B" to "1", "SP-A" to "2"))
        plugin.endDocument("doc1.xml")

        assertThat(plugin.getFirstStopPoints()).containsExactlyInAnyOrder(key("JP-1", "SP-A", "1"), key("JP-2", "SP-B", "1"))
        assertThat(plugin.getLastStopPoints()).containsExactlyInAnyOrder(key("JP-1", "SP-B", "2"), key("JP-2", "SP-A", "2"))
    }

    @Test
    fun shouldExcludeStopPointsThatOccurInMultipleDocuments() {
        journeyPattern("JP-1", listOf("JP-1-1" to "1", "JP-1-2" to "2"))
        plugin.endDocument("doc1.xml")
        journeyPattern("JP-1", listOf("JP-1-1" to "1", "JP-1-3" to "2"))
        plugin.endDocument("doc2.xml")

        assertThat(plugin.getFirstStopPoints()).isEmpty()
        assertThat(plugin.getLastStopPoints()).containsExactlyInAnyOrder(key("JP-1", "JP-1-2", "2"), key("JP-1", "JP-1-3", "2"))
    }

    private fun key(
        journeyPatternId: String,
        id: String,
        order: String?,
    ) = StopPointInJourneyPatternKey(journeyPatternId, id, "any", order)

    private fun journeyPattern(
        id: String,
        stopPoints: List<Pair<String, String?>>,
    ) {
        plugin.startElement(NetexTypes.JOURNEY_PATTERN, attributes("id" to id), null)
        stopPoints.forEach { (spId, order) ->
            val values = listOfNotNull("id" to spId, "version" to "any", order?.let { "order" to it })
            plugin.startElement(NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN, attributes(*values.toTypedArray()), null)
            plugin.endElement(NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN, null)
        }
        plugin.endElement(NetexTypes.JOURNEY_PATTERN, null)
    }

    private fun attributes(vararg values: Pair<String, String>) =
        AttributesImpl().apply {
            values.forEach { (name, value) -> addAttribute("", name, name, "CDATA", value) }
        }
}
