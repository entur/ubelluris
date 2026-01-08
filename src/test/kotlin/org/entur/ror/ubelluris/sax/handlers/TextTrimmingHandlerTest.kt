package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.xml.sax.Attributes

class TextTrimmingHandlerTest {

    private val handler = TextTrimmingHandler()
    private val writer = mock<DelegatingXMLElementWriter>()

    @Test
    fun shouldTrimLeadingWhitespace() {
        val attrs: Attributes = mock()
        val content = "  Test Value".toCharArray()

        handler.startElement(null, "Name", "Name", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "Name", "Name", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(10))

            val transformedContent = String(firstValue, 0, 10)

            assertThat(transformedContent).isEqualTo("Test Value")
        }
    }

    @Test
    fun shouldTrimTrailingWhitespace() {
        val attrs: Attributes = mock()
        val content = "Test Value  ".toCharArray()

        handler.startElement(null, "Name", "Name", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "Name", "Name", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(10))

            val transformedContent = String(firstValue, 0, 10)

            assertThat(transformedContent).isEqualTo("Test Value")
        }
    }

    @Test
    fun shouldTrimBothLeadingAndTrailingWhitespace() {
        val attrs: Attributes = mock()
        val content = "  Test Value  ".toCharArray()

        handler.startElement(null, "Name", "Name", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "Name", "Name", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(10))

            val transformedContent = String(firstValue, 0, 10)

            assertThat(transformedContent).isEqualTo("Test Value")
        }
    }

    @Test
    fun shouldPreserveInternalWhitespace() {
        val attrs: Attributes = mock()
        val content = "  Test  Value  ".toCharArray()

        handler.startElement(null, "Name", "Name", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "Name", "Name", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(11))

            val transformedContent = String(firstValue, 0, 11)

            assertThat(transformedContent).isEqualTo("Test  Value")
        }
    }

    @Test
    fun shouldHandleAlreadyTrimmedText() {
        val attrs: Attributes = mock()
        val content = "Test Value".toCharArray()

        handler.startElement(null, "Name", "Name", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "Name", "Name", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(10))

            val transformedContent = String(firstValue, 0, 10)

            assertThat(transformedContent).isEqualTo("Test Value")
        }
    }
}
