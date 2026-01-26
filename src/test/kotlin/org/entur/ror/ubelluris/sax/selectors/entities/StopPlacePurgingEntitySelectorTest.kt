package org.entur.ror.ubelluris.sax.selectors.entities

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.model.EntityModel
import org.entur.netex.tools.lib.selectors.entities.EntitySelectorContext
import org.entur.ror.ubelluris.data.TestDataFactory.defaultEntity
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.entur.ror.ubelluris.sax.plugins.data.QuayData
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class StopPlacePurgingEntitySelectorTest {

    private val repository = StopPlacePurgingRepository()
    private val selector = StopPlacePurgingEntitySelector(repository)

    private val context = mock<EntitySelectorContext>()
    private val entityModel = mock<EntityModel>()

    @Test
    fun shouldRemoveQuayWhenQuayIdIsInEntityIds() {
        val quay = defaultEntity(id = "quay1", type = NetexTypes.QUAY)
        repository.addEntityId("quay1")

        setupEntities(mapOf(NetexTypes.QUAY to mapOf("quay1" to quay)))

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.QUAY]).isEmpty()
    }

    @Test
    fun shouldKeepQuayWhenQuayIdIsNotInEntityIds() {
        val quay = defaultEntity(id = "quay1", type = NetexTypes.QUAY)

        setupEntities(mapOf(NetexTypes.QUAY to mapOf("quay1" to quay)))

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.QUAY]).containsKey("quay1")
    }

    @Test
    fun shouldRemoveStopPlaceWithSingleQuayWithIllegalPublicCode() {
        val stopPlace = defaultEntity(id = "stopPlace1", type = NetexTypes.STOP_PLACE)
        repository.addQuayToStopPlace("stopPlace1", QuayData("quay1", "*"))

        setupEntities(mapOf(NetexTypes.STOP_PLACE to mapOf("stopPlace1" to stopPlace)))

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).isEmpty()
    }

    @Test
    fun shouldRemoveStopPlaceWithSingleQuayWithDashPublicCode() {
        val stopPlace = defaultEntity(id = "stopPlace1", type = NetexTypes.STOP_PLACE)
        repository.addQuayToStopPlace("stopPlace1", QuayData("quay1", "-"))

        setupEntities(mapOf(NetexTypes.STOP_PLACE to mapOf("stopPlace1" to stopPlace)))

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).isEmpty()
    }

    @Test
    fun shouldKeepStopPlaceWithSingleQuayWithValidPublicCode() {
        val stopPlace = defaultEntity(id = "stopPlace1", type = NetexTypes.STOP_PLACE)
        repository.addQuayToStopPlace("stopPlace1", QuayData("quay1", "A"))

        setupEntities(mapOf(NetexTypes.STOP_PLACE to mapOf("stopPlace1" to stopPlace)))

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).containsKey("stopPlace1")
    }

    @Test
    fun shouldRemoveChildStopPlaceWithNoQuays() {
        val childStopPlace = defaultEntity(id = "childStop1", type = NetexTypes.STOP_PLACE)
        repository.addChildStopToParent("parentStop1", "childStop1")

        setupEntities(mapOf(NetexTypes.STOP_PLACE to mapOf("childStop1" to childStopPlace)))

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).isEmpty()
    }

    @Test
    fun shouldRemoveOrphanedStopPlaceWithNoQuays() {
        val stopPlace = defaultEntity(id = "orphanedStop", type = NetexTypes.STOP_PLACE)

        setupEntities(mapOf(NetexTypes.STOP_PLACE to mapOf("orphanedStop" to stopPlace)))

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).isEmpty()
    }

    @Test
    fun shouldKeepParentStopPlaceWithNoQuaysButMultipleChildren() {
        val parentStopPlace = defaultEntity(id = "parentStop", type = NetexTypes.STOP_PLACE)
        val child1 = defaultEntity(id = "child1", type = NetexTypes.STOP_PLACE)
        val child2 = defaultEntity(id = "child2", type = NetexTypes.STOP_PLACE)

        repository.addChildStopToParent("parentStop", "child1")
        repository.addChildStopToParent("parentStop", "child2")
        repository.addQuayToStopPlace("child1", QuayData("quay1", "A"))
        repository.addQuayToStopPlace("child2", QuayData("quay2", "B"))

        setupEntities(
            mapOf(
                NetexTypes.STOP_PLACE to mapOf(
                    "parentStop" to parentStopPlace,
                    "child1" to child1,
                    "child2" to child2
                )
            )
        )

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).containsKey("parentStop")
        assertThat(result.selection[NetexTypes.STOP_PLACE]).containsKey("child1")
        assertThat(result.selection[NetexTypes.STOP_PLACE]).containsKey("child2")
    }

    @Test
    fun shouldRemoveParentStopPlaceWithNoQuaysAndOnlyOneRemainingChild() {
        val parentStopPlace = defaultEntity(id = "parentStop", type = NetexTypes.STOP_PLACE)
        val child1 = defaultEntity(id = "child1", type = NetexTypes.STOP_PLACE)

        repository.addChildStopToParent("parentStop", "child1")
        repository.addQuayToStopPlace("child1", QuayData("quay1", "A"))

        setupEntities(
            mapOf(
                NetexTypes.STOP_PLACE to mapOf(
                    "parentStop" to parentStopPlace,
                    "child1" to child1
                )
            )
        )

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).doesNotContainKey("parentStop")
        assertThat(result.selection[NetexTypes.STOP_PLACE]).containsKey("child1")
    }

    @Test
    fun shouldRemoveParentStopPlaceWhenAllChildrenAreRemoved() {
        val parentStopPlace = defaultEntity(id = "parentStop", type = NetexTypes.STOP_PLACE)
        val child1 = defaultEntity(id = "child1", type = NetexTypes.STOP_PLACE)

        repository.addChildStopToParent("parentStop", "child1")

        setupEntities(
            mapOf(
                NetexTypes.STOP_PLACE to mapOf(
                    "parentStop" to parentStopPlace,
                    "child1" to child1
                )
            )
        )

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).isEmpty()
    }

    @Test
    fun shouldKeepStopPlaceWithMultipleQuays() {
        val stopPlace = defaultEntity(id = "stopPlace1", type = NetexTypes.STOP_PLACE)
        repository.addQuayToStopPlace("stopPlace1", QuayData("quay1", "A"))
        repository.addQuayToStopPlace("stopPlace1", QuayData("quay2", "B"))

        setupEntities(mapOf(NetexTypes.STOP_PLACE to mapOf("stopPlace1" to stopPlace)))

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).containsKey("stopPlace1")
    }

    @Test
    fun shouldKeepStopPlaceWithMultipleQuaysEvenWhenOneHasIllegalPublicCode() {
        val stopPlace = defaultEntity(id = "stopPlace1", type = NetexTypes.STOP_PLACE)
        repository.addQuayToStopPlace("stopPlace1", QuayData("quay1", "*"))
        repository.addQuayToStopPlace("stopPlace1", QuayData("quay2", "A"))

        setupEntities(mapOf(NetexTypes.STOP_PLACE to mapOf("stopPlace1" to stopPlace)))

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.STOP_PLACE]).containsKey("stopPlace1")
    }

    @Test
    fun shouldRemoveQuayAndUpdateStopPlaceAccordingly() {
        val stopPlace = defaultEntity(id = "stopPlace1", type = NetexTypes.STOP_PLACE)
        val quay1 = defaultEntity(id = "quay1", type = NetexTypes.QUAY)
        val quay2 = defaultEntity(id = "quay2", type = NetexTypes.QUAY)

        repository.addQuayToStopPlace("stopPlace1", QuayData("quay1", "A"))
        repository.addQuayToStopPlace("stopPlace1", QuayData("quay2", "B"))
        repository.addEntityId("quay1")

        setupEntities(
            mapOf(
                NetexTypes.STOP_PLACE to mapOf("stopPlace1" to stopPlace),
                NetexTypes.QUAY to mapOf("quay1" to quay1, "quay2" to quay2)
            )
        )

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.QUAY]).containsKey("quay2")
        assertThat(result.selection[NetexTypes.QUAY]).doesNotContainKey("quay1")
        assertThat(result.selection[NetexTypes.STOP_PLACE]).containsKey("stopPlace1")
    }

    @Test
    fun shouldRemoveStopPlaceWhenAllQuaysAreRemoved() {
        val stopPlace = defaultEntity(id = "stopPlace1", type = NetexTypes.STOP_PLACE)
        val quay1 = defaultEntity(id = "quay1", type = NetexTypes.QUAY)

        repository.addQuayToStopPlace("stopPlace1", QuayData("quay1", "A"))
        repository.addEntityId("quay1")

        setupEntities(
            mapOf(
                NetexTypes.STOP_PLACE to mapOf("stopPlace1" to stopPlace),
                NetexTypes.QUAY to mapOf("quay1" to quay1)
            )
        )

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.QUAY]).isEmpty()
        assertThat(result.selection[NetexTypes.STOP_PLACE]).isEmpty()
    }

    @Test
    fun shouldKeepOtherEntityTypesUnchanged() {
        val line = defaultEntity(id = "line1", type = NetexTypes.LINE)

        setupEntities(mapOf(NetexTypes.LINE to mapOf("line1" to line)))

        val result = selector.selectEntities(context)

        assertThat(result.selection[NetexTypes.LINE]).containsKey("line1")
    }

    @Test
    fun shouldHandleEmptyEntities() {
        setupEntities(emptyMap())

        val result = selector.selectEntities(context)

        assertThat(result.selection).isEmpty()
    }

    private fun setupEntities(entities: Map<String, Map<String, Entity>>) {
        val mutableEntities = entities.mapValues { (_, v) -> v.toMutableMap() }.toMutableMap()
        whenever(context.entityModel).thenReturn(entityModel)
        whenever(entityModel.getEntitesByTypeAndId()).thenReturn(mutableEntities)
    }
}