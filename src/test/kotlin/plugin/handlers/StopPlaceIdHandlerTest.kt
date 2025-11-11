package plugin.handlers

import model.NetexTypes
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl
import kotlin.test.assertEquals

class StopPlaceIdHandlerTest {
    private val handler = StopPlaceIdHandler()
    private val writer = mock<DelegatingXMLElementWriter>()

    @Test
    fun testStopPlaceIdHandler() {
        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:StopPlace:1")
        whenever(attrs.getValue("version")).thenReturn("42")

        handler.startElement(null, "StopPlace", "StopPlace", attrs, writer)

        argumentCaptor<AttributesImpl>().apply {
            verify(writer).startElement(
                eq(null),
                eq(NetexTypes.STOP_PLACE),
                eq(NetexTypes.STOP_PLACE),
                capture()
            )

            val rewrittenId = firstValue.getValue("id")
            val version = firstValue.getValue("version")

            assertEquals("SAM:StopPlace:1", rewrittenId)
            assertEquals("1", version)
        }
    }
}