package org.entur.ror.ubelluris.sax.plugins.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.data.TestDataFactory.defaultEntity
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingParsingContext
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.xml.sax.Attributes

class ParentSiteRefHandlerTest {
    private val context = StopPlacePurgingParsingContext()

    private val stopPlacePurgingRepository = StopPlacePurgingRepository()

    private val parentSiteRefHandler = ParentSiteRefHandler(stopPlacePurgingRepository)

    @Test
    fun shouldAddChildToParentWhenRefAttributeExists() {
        val childStopPlace = defaultEntity(id = "SE:050:StopPlace:123", type = NetexTypes.STOP_PLACE)
        val attrs: Attributes = mock()
        whenever(attrs.getValue("ref")).thenReturn("SE:050:StopPlace:ParentId")

        parentSiteRefHandler.startElement(context, attrs, childStopPlace)
        parentSiteRefHandler.endElement(context, childStopPlace)

        assertThat(stopPlacePurgingRepository.parentSiteRefsPerStopPlace)
            .containsKey("SE:050:StopPlace:ParentId")
        assertThat(stopPlacePurgingRepository.parentSiteRefsPerStopPlace["SE:050:StopPlace:ParentId"])
            .containsOnly("SE:050:StopPlace:123")
        assertThat(stopPlacePurgingRepository.childStopPlaces)
            .containsOnly("SE:050:StopPlace:123")
        assertThat(stopPlacePurgingRepository.isChildStopPlace("SE:050:StopPlace:123"))
            .isTrue()
    }

    @Test
    fun shouldNotAddWhenRefIsNull() {
        val childStopPlace = defaultEntity(id = "SE:050:StopPlace:111", type = NetexTypes.STOP_PLACE)
        val attrs: Attributes = mock()
        whenever(attrs.getValue("ref")).thenReturn(null)

        parentSiteRefHandler.startElement(context, attrs, childStopPlace)
        parentSiteRefHandler.endElement(context, childStopPlace)

        assertThat(stopPlacePurgingRepository.parentSiteRefsPerStopPlace).isEmpty()
        assertThat(stopPlacePurgingRepository.childStopPlaces).isEmpty()
    }

}