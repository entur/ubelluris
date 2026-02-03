package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.ror.ubelluris.model.NetexTypes
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.xml.sax.Attributes

class XmlnsHandlerTest {
    private val handler = XmlnsHandler("SE:050", "SAM")

    private val writer = mock<DelegatingXMLElementWriter>()

    @Test
    fun testXmlnsHandler() {
        val attrs: Attributes = mock()
        val content = "SE:050".toCharArray()

        handler.startElement(null, NetexTypes.XML_NS, NetexTypes.XML_NS, attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, NetexTypes.XML_NS, NetexTypes.XML_NS, writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(3))

            val transformedContent = String(firstValue, 0, 3)
            assertThat(transformedContent).isEqualTo("SAM")
        }
    }
}