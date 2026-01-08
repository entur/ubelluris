package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.xml.sax.Attributes

class PublicCodeFilterHandlerTest {

    private val illegalCodes = listOf("*", "-", "81", "82", "83")
    private val handler = PublicCodeFilterHandler(illegalCodes)
    private val writer = mock<DelegatingXMLElementWriter>()

    @Test
    fun shouldReplaceAsteriskWithEmptyString() {
        val attrs: Attributes = mock()
        val content = "*".toCharArray()

        handler.startElement(null, "PublicCode", "PublicCode", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "PublicCode", "PublicCode", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(0))

            val transformedContent = String(firstValue, 0, 0)

            assertThat(transformedContent).isEmpty()
        }
    }

    @Test
    fun shouldReplaceDashWithEmptyString() {
        val attrs: Attributes = mock()
        val content = "-".toCharArray()

        handler.startElement(null, "PublicCode", "PublicCode", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "PublicCode", "PublicCode", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(0))

            val transformedContent = String(firstValue, 0, 0)

            assertThat(transformedContent).isEmpty()
        }
    }

    @Test
    fun shouldReplaceIllegalCode81WithEmptyString() {
        val attrs: Attributes = mock()
        val content = "81".toCharArray()

        handler.startElement(null, "PublicCode", "PublicCode", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "PublicCode", "PublicCode", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(0))

            val transformedContent = String(firstValue, 0, 0)

            assertThat(transformedContent).isEmpty()
        }
    }

    @Test
    fun shouldPreserveValidPublicCode() {
        val attrs: Attributes = mock()
        val content = "A123".toCharArray()

        handler.startElement(null, "PublicCode", "PublicCode", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "PublicCode", "PublicCode", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(4))

            val transformedContent = String(firstValue, 0, 4)

            assertThat(transformedContent).isEqualTo("A123")
        }
    }

    @Test
    fun shouldHandleWhitespaceAroundIllegalCode() {
        val attrs: Attributes = mock()
        val content = "  *  ".toCharArray()

        handler.startElement(null, "PublicCode", "PublicCode", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "PublicCode", "PublicCode", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(0))

            val transformedContent = String(firstValue, 0, 0)

            assertThat(transformedContent).isEmpty()
        }
    }

    @Test
    fun shouldPreserveNumericCodesThatAreNotIllegal() {
        val attrs: Attributes = mock()
        val content = "123".toCharArray()

        handler.startElement(null, "PublicCode", "PublicCode", attrs, writer)
        handler.characters(content, 0, content.size, writer)
        handler.endElement(null, "PublicCode", "PublicCode", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(3))

            val transformedContent = String(firstValue, 0, 3)

            assertThat(transformedContent).isEqualTo("123")
        }
    }
}
