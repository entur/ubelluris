package org.entur.ror.ubelluris.sax.plugins

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.sax.plugins.data.ServiceJourneyInterchangeData
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

class ServiceJourneyInterchangeCollectorPluginTest {
    @Test
    fun shouldIdentifyIdenticalDuplicates() {
        val plugin = ServiceJourneyInterchangeCollectorPlugin()

        val id = "SE:013:ServiceJourneyInterchange:duplicate_id"
        val data1 =
            ServiceJourneyInterchangeData(
                id = id,
                version = "any",
                priority = "0",
                guaranteed = "false",
                advertised = "true",
                fromPointRef = "SE:013:ScheduledStopPoint:9022013003003001",
                toPointRef = "SE:013:ScheduledStopPoint:9022013003003001",
                fromJourneyRef = "SE:013:ServiceJourney:130000000000001353",
                toJourneyRef = "SE:013:ServiceJourney:130000000000006580",
            )
        val data2 = data1.copy()

        // Use reflection to set the interchangeData field
        setInterchangeData(plugin, mapOf(id to mutableListOf(data1, data2)))

        val identicalDuplicates = plugin.getIdenticalDuplicates()
        val conflictingDuplicates = plugin.getConflictingDuplicates()

        assertThat(identicalDuplicates).containsExactly(id)
        assertThat(conflictingDuplicates).isEmpty()
    }

    @Test
    fun shouldIdentifyConflictingDuplicates() {
        val plugin = ServiceJourneyInterchangeCollectorPlugin()

        val id = "SE:013:ServiceJourneyInterchange:conflicting_id"
        val data1 =
            ServiceJourneyInterchangeData(
                id = id,
                version = "any",
                priority = "0",
                guaranteed = "false",
                advertised = "true",
                fromPointRef = "SE:013:ScheduledStopPoint:9022013003003001",
                toPointRef = "SE:013:ScheduledStopPoint:9022013003003001",
                fromJourneyRef = "SE:013:ServiceJourney:130000000000001353",
                toJourneyRef = "SE:013:ServiceJourney:130000000000006580",
            )
        val data2 = data1.copy(priority = "1") // Different priority

        setInterchangeData(plugin, mapOf(id to mutableListOf(data1, data2)))

        val identicalDuplicates = plugin.getIdenticalDuplicates()
        val conflictingDuplicates = plugin.getConflictingDuplicates()

        assertThat(identicalDuplicates).isEmpty()
        assertThat(conflictingDuplicates).containsExactly(id)
    }

    @Test
    fun shouldHandleMultipleIdenticalDuplicates() {
        val plugin = ServiceJourneyInterchangeCollectorPlugin()

        val id = "SE:013:ServiceJourneyInterchange:triple_duplicate"
        val data1 =
            ServiceJourneyInterchangeData(
                id = id,
                version = "any",
                priority = "0",
                guaranteed = "false",
                advertised = "true",
                fromPointRef = "SE:013:ScheduledStopPoint:9022013003003001",
                toPointRef = "SE:013:ScheduledStopPoint:9022013003003001",
                fromJourneyRef = "SE:013:ServiceJourney:130000000000001353",
                toJourneyRef = "SE:013:ServiceJourney:130000000000006580",
            )
        val data2 = data1.copy()
        val data3 = data1.copy()

        setInterchangeData(plugin, mapOf(id to mutableListOf(data1, data2, data3)))

        val identicalDuplicates = plugin.getIdenticalDuplicates()

        assertThat(identicalDuplicates).containsExactly(id)
    }

    @Test
    fun shouldNotMarkSingleOccurrenceAsDuplicate() {
        val plugin = ServiceJourneyInterchangeCollectorPlugin()

        val id = "SE:013:ServiceJourneyInterchange:unique_id"
        val data =
            ServiceJourneyInterchangeData(
                id = id,
                version = "any",
                priority = "0",
                guaranteed = "false",
                advertised = "true",
                fromPointRef = "SE:013:ScheduledStopPoint:9022013003003001",
                toPointRef = "SE:013:ScheduledStopPoint:9022013003003001",
                fromJourneyRef = "SE:013:ServiceJourney:130000000000001353",
                toJourneyRef = "SE:013:ServiceJourney:130000000000006580",
            )

        setInterchangeData(plugin, mapOf(id to mutableListOf(data)))

        val identicalDuplicates = plugin.getIdenticalDuplicates()
        val conflictingDuplicates = plugin.getConflictingDuplicates()

        assertThat(identicalDuplicates).isEmpty()
        assertThat(conflictingDuplicates).isEmpty()
    }

    @Test
    fun shouldHandleMixOfIdenticalAndConflictingDuplicates() {
        val plugin = ServiceJourneyInterchangeCollectorPlugin()

        val identicalId = "SE:013:ServiceJourneyInterchange:identical_id"
        val conflictingId = "SE:013:ServiceJourneyInterchange:conflicting_id"

        val identicalData1 =
            ServiceJourneyInterchangeData(
                id = identicalId,
                version = "any",
                priority = "0",
                guaranteed = "false",
                advertised = "true",
                fromPointRef = "SE:013:ScheduledStopPoint:9022013003003001",
                toPointRef = "SE:013:ScheduledStopPoint:9022013003003001",
                fromJourneyRef = "SE:013:ServiceJourney:130000000000001353",
                toJourneyRef = "SE:013:ServiceJourney:130000000000006580",
            )
        val identicalData2 = identicalData1.copy()

        val conflictingData1 = identicalData1.copy(id = conflictingId)
        val conflictingData2 = conflictingData1.copy(guaranteed = "true")

        setInterchangeData(
            plugin,
            mapOf(
                identicalId to mutableListOf(identicalData1, identicalData2),
                conflictingId to mutableListOf(conflictingData1, conflictingData2),
            ),
        )

        val identicalDuplicates = plugin.getIdenticalDuplicates()
        val conflictingDuplicates = plugin.getConflictingDuplicates()

        assertThat(identicalDuplicates).containsExactly(identicalId)
        assertThat(conflictingDuplicates).containsExactly(conflictingId)
    }

    private fun setInterchangeData(
        plugin: ServiceJourneyInterchangeCollectorPlugin,
        data: Map<String, MutableList<ServiceJourneyInterchangeData>>,
    ) {
        val field: Field = plugin.javaClass.getDeclaredField("interchangeData")
        field.isAccessible = true
        field.set(plugin, data.toMutableMap())
    }
}
