package org.entur.ror.ubelluris.sax.plugins.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.data.TestDataFactory.defaultEntity
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingParsingContext
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.entur.ror.ubelluris.sax.plugins.data.QuayData
import org.junit.jupiter.api.Test

class PublicCodeHandlerTest {
    private val context = StopPlacePurgingParsingContext()

    private val stopPlacePurgingRepository = StopPlacePurgingRepository()

    private val publicCodeHandler = PublicCodeHandler(stopPlacePurgingRepository)

    @Test
    fun shouldAddEntityIdIfBlacklistedPublicCode() {
        val quayEntity = defaultEntity(id = "entityId", type = NetexTypes.QUAY)

        publicCodeHandler.startElement(context, null, quayEntity)
        publicCodeHandler.characters(context, "81".toCharArray(), 0, 2)
        publicCodeHandler.endElement(context, quayEntity)

        assertThat(stopPlacePurgingRepository.entityIds).containsOnly("entityId")
    }

    @Test
    fun shouldNotAddEntityIdIfBlacklistedPublicCode() {
        val quayEntity = defaultEntity(id = "entityId", type = NetexTypes.QUAY)

        publicCodeHandler.startElement(context, null, quayEntity)
        publicCodeHandler.characters(context, "A".toCharArray(), 0, 1)
        publicCodeHandler.endElement(context, quayEntity)

        assertThat(stopPlacePurgingRepository.entityIds).isEmpty()
    }

    @Test
    fun shouldAddQuayToStopPlaceWhenParentExists() {
        val parentEntity = defaultEntity(id = "stopPlaceId", type = NetexTypes.STOP_PLACE)
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY, parent = parentEntity)

        publicCodeHandler.startElement(context, null, quayEntity)
        publicCodeHandler.characters(context, "A".toCharArray(), 0, 1)
        publicCodeHandler.endElement(context, quayEntity)

        assertThat(stopPlacePurgingRepository.quaysPerStopPlace)
            .containsKey("stopPlaceId")
        assertThat(stopPlacePurgingRepository.quaysPerStopPlace["stopPlaceId"])
            .containsExactly(QuayData("quayId", "A"))
    }

    @Test
    fun shouldSetQuayHasPublicCodeWhenParentExists() {
        val parentEntity = defaultEntity(id = "stopPlaceId", type = NetexTypes.STOP_PLACE)
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY, parent = parentEntity)

        publicCodeHandler.startElement(context, null, quayEntity)
        publicCodeHandler.characters(context, "B".toCharArray(), 0, 1)
        publicCodeHandler.endElement(context, quayEntity)

        assertThat(context.quayHasPublicCode).isEqualTo(true)
    }

    @Test
    fun shouldNotAddQuayToStopPlaceWhenNoParent() {
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY)

        publicCodeHandler.startElement(context, null, quayEntity)
        publicCodeHandler.characters(context, "A".toCharArray(), 0, 1)
        publicCodeHandler.endElement(context, quayEntity)

        assertThat(stopPlacePurgingRepository.quaysPerStopPlace).isEmpty()
        assertThat(context.quayHasPublicCode).isEqualTo(false)
    }

}