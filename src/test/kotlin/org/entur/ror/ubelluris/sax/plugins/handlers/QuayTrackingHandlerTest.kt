package org.entur.ror.ubelluris.sax.plugins.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.data.TestDataFactory.defaultEntity
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingParsingContext
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.entur.ror.ubelluris.sax.plugins.data.QuayData
import org.junit.jupiter.api.Test

class QuayTrackingHandlerTest {

    private val context = StopPlacePurgingParsingContext()

    private val stopPlacePurgingRepository = StopPlacePurgingRepository()

    private val quayTrackingHandler = QuayTrackingHandler(stopPlacePurgingRepository)

    @Test
    fun shouldSetCurrentQuayIdOnStartElement() {
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY)

        quayTrackingHandler.startElement(context, null, quayEntity)

        assertThat(context.currentQuayId).isEqualTo("quayId")
    }

    @Test
    fun shouldResetQuayHasPublicCodeOnStartElement() {
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY)
        context.quayHasPublicCode = true

        quayTrackingHandler.startElement(context, null, quayEntity)

        assertThat(context.quayHasPublicCode).isEqualTo(false)
    }

    @Test
    fun shouldAddQuayToStopPlaceWhenNoPublicCodeAndParentExists() {
        val parentEntity = defaultEntity(id = "stopPlaceId", type = NetexTypes.STOP_PLACE)
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY, parent = parentEntity)

        quayTrackingHandler.startElement(context, null, quayEntity)
        quayTrackingHandler.endElement(context, quayEntity)

        assertThat(stopPlacePurgingRepository.quaysPerStopPlace)
            .containsKey("stopPlaceId")
        assertThat(stopPlacePurgingRepository.quaysPerStopPlace["stopPlaceId"])
            .containsExactly(QuayData("quayId", ""))
    }

    @Test
    fun shouldNotAddQuayToStopPlaceWhenHasPublicCode() {
        val parentEntity = defaultEntity(id = "stopPlaceId", type = NetexTypes.STOP_PLACE)
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY, parent = parentEntity)

        quayTrackingHandler.startElement(context, null, quayEntity)
        context.quayHasPublicCode = true
        quayTrackingHandler.endElement(context, quayEntity)

        assertThat(stopPlacePurgingRepository.quaysPerStopPlace).isEmpty()
    }

    @Test
    fun shouldNotAddQuayToStopPlaceWhenNoParent() {
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY)

        quayTrackingHandler.startElement(context, null, quayEntity)
        quayTrackingHandler.endElement(context, quayEntity)

        assertThat(stopPlacePurgingRepository.quaysPerStopPlace).isEmpty()
    }

    @Test
    fun shouldResetContextOnEndElement() {
        val parentEntity = defaultEntity(id = "stopPlaceId", type = NetexTypes.STOP_PLACE)
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY, parent = parentEntity)

        quayTrackingHandler.startElement(context, null, quayEntity)
        context.quayHasPublicCode = true
        quayTrackingHandler.endElement(context, quayEntity)

        assertThat(context.currentQuayId).isNull()
        assertThat(context.quayHasPublicCode).isEqualTo(false)
    }
}