package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.xml.sax.Attributes

class CoordinateRoundingHandlerTest {
    private val handler = CoordinateRoundingHandler()
    private val writer = mock<DelegatingXMLElementWriter>()

    private fun process(vararg chunks: String): String {
        val attrs: Attributes = mock()
        handler.startElement(null, "Latitude", "Latitude", attrs, writer)
        chunks.forEach { chunk ->
            val content = chunk.toCharArray()
            handler.characters(content, 0, content.size, writer)
        }
        handler.endElement(null, "Latitude", "Latitude", writer)

        return argumentCaptor<CharArray>().run {
            val lengthCaptor = argumentCaptor<Int>()
            verify(writer).characters(capture(), eq(0), lengthCaptor.capture())
            String(firstValue, 0, lengthCaptor.firstValue)
        }
    }

    @Test
    fun shouldRoundHalfUpWhenMoreThanSixDecimals() {
        assertThat(process("59.9123455")).isEqualTo("59.912346")
    }

    @Test
    fun shouldRoundDownWhenBelowHalf() {
        assertThat(process("10.7522224999")).isEqualTo("10.752222")
    }

    @Test
    fun shouldRoundNegativeValuesHalfUpAwayFromZero() {
        assertThat(process("-59.9123455")).isEqualTo("-59.912346")
    }

    @Test
    fun shouldNotChangeValueWithExactlySixDecimals() {
        assertThat(process("59.912345")).isEqualTo("59.912345")
    }

    @Test
    fun shouldNotPadValueWithFewerThanSixDecimals() {
        assertThat(process("59.91")).isEqualTo("59.91")
    }
}
