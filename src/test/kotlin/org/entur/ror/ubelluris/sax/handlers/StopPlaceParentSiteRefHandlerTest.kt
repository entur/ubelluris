package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.ror.ubelluris.model.NetexTypes
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

class StopPlaceParentSiteRefHandlerTest {
    private val handler = StopPlaceParentSiteRefHandler()
    private val writer = mock<DelegatingXMLElementWriter>()

    @Test
    fun testStopPlaceParentSiteRefHandler() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("ref")).thenReturn("SE:050:StopPlace:1")

        handler.startElement(null, "ref", "ref", attrs, writer)

        argumentCaptor<AttributesImpl>().apply {
            verify(writer).startElement(
                eq(null),
                eq(NetexTypes.PARENT_SITE_REF),
                eq(NetexTypes.PARENT_SITE_REF),
                capture()
            )

            val rewrittenId = firstValue.getValue("ref")

            assertThat(rewrittenId).isEqualTo("SAM:StopPlace:1")
        }
    }
}