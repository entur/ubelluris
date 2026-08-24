package org.entur.ror.ubelluris.sax.selectors.entities

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.model.EntityModel
import org.entur.netex.tools.lib.selectors.entities.EntitySelectorContext
import org.entur.ror.ubelluris.data.TestDataFactory.defaultEntity
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.ServiceJourneyInterchangeCollectorPlugin
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class ServiceJourneyInterchangeDeduplicationSelectorTest {
    private val collectorPlugin = mock<ServiceJourneyInterchangeCollectorPlugin>()
    private val selector = ServiceJourneyInterchangeDeduplicationSelector(collectorPlugin)
    private val context = mock<EntitySelectorContext>()
    private val entityModel = mock<EntityModel>()

    @Test
    fun shouldKeepFirstOccurrenceOfIdenticalDuplicates() {
        val interchange1 =
            defaultEntity(
                id = "SE:013:ServiceJourneyInterchange:duplicate_id",
                type = NetexTypes.SERVICE_JOURNEY_INTERCHANGE,
            )
        val interchange2 =
            defaultEntity(
                id = "SE:013:ServiceJourneyInterchange:unique_id",
                type = NetexTypes.SERVICE_JOURNEY_INTERCHANGE,
            )

        // Mock the plugin to return that duplicate_id has identical duplicates
        whenever(collectorPlugin.getIdenticalDuplicates()).thenReturn(setOf("SE:013:ServiceJourneyInterchange:duplicate_id"))
        whenever(collectorPlugin.getConflictingDuplicates()).thenReturn(emptySet())

        setupEntities(
            mapOf(
                NetexTypes.SERVICE_JOURNEY_INTERCHANGE to
                    mapOf(
                        interchange1.id to interchange1,
                        interchange2.id to interchange2,
                    ),
            ),
        )

        val result = selector.selectEntities(context)

        val interchanges = result.selection[NetexTypes.SERVICE_JOURNEY_INTERCHANGE]
        // Should keep both: the first occurrence of duplicate_id and the unique one
        assertThat(interchanges).hasSize(2)
        assertThat(interchanges).containsKey(interchange1.id)
        assertThat(interchanges).containsKey(interchange2.id)
    }

    @Test
    fun shouldRemoveAllOccurrencesOfConflictingDuplicates() {
        val interchange1 =
            defaultEntity(
                id = "SE:013:ServiceJourneyInterchange:conflicting_id",
                type = NetexTypes.SERVICE_JOURNEY_INTERCHANGE,
            )
        val interchange2 =
            defaultEntity(
                id = "SE:013:ServiceJourneyInterchange:unique_id",
                type = NetexTypes.SERVICE_JOURNEY_INTERCHANGE,
            )

        // Mock the plugin to return that conflicting_id has conflicting duplicates
        whenever(collectorPlugin.getIdenticalDuplicates()).thenReturn(emptySet())
        whenever(collectorPlugin.getConflictingDuplicates()).thenReturn(setOf("SE:013:ServiceJourneyInterchange:conflicting_id"))

        setupEntities(
            mapOf(
                NetexTypes.SERVICE_JOURNEY_INTERCHANGE to
                    mapOf(
                        interchange1.id to interchange1,
                        interchange2.id to interchange2,
                    ),
            ),
        )

        val result = selector.selectEntities(context)

        val interchanges = result.selection[NetexTypes.SERVICE_JOURNEY_INTERCHANGE]
        // Should only keep unique_id, conflicting_id should be completely removed
        assertThat(interchanges).hasSize(1)
        assertThat(interchanges).containsKey(interchange2.id)
        assertThat(interchanges).doesNotContainKey(interchange1.id)
    }

    @Test
    fun shouldKeepAllUniqueServiceJourneyInterchanges() {
        val interchange1 =
            defaultEntity(
                id = "SE:013:ServiceJourneyInterchange:A_9022013003003001_130000000000001353_130000000000006580",
                type = NetexTypes.SERVICE_JOURNEY_INTERCHANGE,
            )
        val interchange2 =
            defaultEntity(
                id = "SE:013:ServiceJourneyInterchange:A_9022013003003001_130000000000001361_130000000000006580",
                type = NetexTypes.SERVICE_JOURNEY_INTERCHANGE,
            )

        // Mock the plugin to return no duplicates
        whenever(collectorPlugin.getIdenticalDuplicates()).thenReturn(emptySet())
        whenever(collectorPlugin.getConflictingDuplicates()).thenReturn(emptySet())

        setupEntities(
            mapOf(
                NetexTypes.SERVICE_JOURNEY_INTERCHANGE to
                    mapOf(
                        interchange1.id to interchange1,
                        interchange2.id to interchange2,
                    ),
            ),
        )

        val result = selector.selectEntities(context)

        val interchanges = result.selection[NetexTypes.SERVICE_JOURNEY_INTERCHANGE]
        assertThat(interchanges).hasSize(2)
        assertThat(interchanges).containsKey(interchange1.id)
        assertThat(interchanges).containsKey(interchange2.id)
    }

    @Test
    fun shouldKeepOtherEntityTypesUnchanged() {
        val stopPlace = defaultEntity(id = "stopPlace1", type = NetexTypes.STOP_PLACE)
        val quay = defaultEntity(id = "quay1", type = NetexTypes.QUAY)
        val interchange =
            defaultEntity(
                id = "SE:013:ServiceJourneyInterchange:A_9022013003003001_130000000000001353_130000000000006580",
                type = NetexTypes.SERVICE_JOURNEY_INTERCHANGE,
            )

        whenever(collectorPlugin.getIdenticalDuplicates()).thenReturn(emptySet())
        whenever(collectorPlugin.getConflictingDuplicates()).thenReturn(emptySet())

        setupEntities(
            mapOf(
                NetexTypes.STOP_PLACE to mapOf(stopPlace.id to stopPlace),
                NetexTypes.QUAY to mapOf(quay.id to quay),
                NetexTypes.SERVICE_JOURNEY_INTERCHANGE to mapOf(interchange.id to interchange),
            ),
        )

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).containsKey(stopPlace.id)
        assertThat(result.selection[NetexTypes.QUAY]).containsKey(quay.id)
        assertThat(result.selection[NetexTypes.SERVICE_JOURNEY_INTERCHANGE]).containsKey(interchange.id)
    }

    @Test
    fun shouldHandleEmptyServiceJourneyInterchangeList() {
        whenever(collectorPlugin.getIdenticalDuplicates()).thenReturn(emptySet())
        whenever(collectorPlugin.getConflictingDuplicates()).thenReturn(emptySet())

        setupEntities(emptyMap())

        val result = selector.selectEntities(context)

        assertThat(result.selection).isEmpty()
    }

    private fun setupEntities(entitiesByType: Map<String, Map<String, Entity>>) {
        whenever(context.entityModel).thenReturn(entityModel)
        val mutableMap = entitiesByType.mapValues { (_, entities) -> entities.toMutableMap() }.toMutableMap()
        whenever(entityModel.getEntitesByTypeAndId()).thenReturn(mutableMap)
    }
}
