package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.handlers.util.AttributeReplacer
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

class StopPlaceQuayHandlerTest {

    private val attributeReplacer = AttributeReplacer("SE:050", "SAM")
    private val handler = StopPlaceQuayHandler(attributeReplacer)
    private val writer = mock<DelegatingXMLElementWriter>()

    @Test
    fun testStopPlaceQuayHandler() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:Quay:1")
        whenever(attrs.getValue("version")).thenReturn("42")

        handler.startElement(null, "Quay", "Quay", attrs, writer)

        argumentCaptor<AttributesImpl>().apply {
            verify(writer).startElement(
                eq(null),
                eq(NetexTypes.QUAY),
                eq(NetexTypes.QUAY),
                capture()
            )

            val rewrittenId = firstValue.getValue("id")
            val version = firstValue.getValue("version")

            assertThat(rewrittenId).isEqualTo("SAM:Quay:1")
            assertThat(version).isEqualTo("1")
        }
    }
}