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

class XmlnsUrlHandlerTest {
    private val expectedUrl = "http://www.foo.com"

    private val handler = XmlnsUrlHandler(expectedUrl)

    private val writer = mock<DelegatingXMLElementWriter>()


    @Test
    fun testXmlnsUrlHandler() {
        val attrs: Attributes = mock()
        val content = "SOME_CONTENT".toCharArray()

        handler.startElement(null, NetexTypes.XML_NS_URL, NetexTypes.XML_NS_URL, attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, NetexTypes.XML_NS_URL, NetexTypes.XML_NS_URL, writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(expectedUrl.length))

            val transformedContent = String(firstValue, 0, expectedUrl.length)
            assertThat(transformedContent).isEqualTo("http://www.foo.com")
        }
    }

}