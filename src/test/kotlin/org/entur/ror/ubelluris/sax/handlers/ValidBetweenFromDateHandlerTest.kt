package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.xml.sax.Attributes
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class ValidBetweenFromDateHandlerTest {

    private val zoneId = ZoneId.systemDefault()
    private val fixedTime = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
    private val clock = Clock.fixed(
        fixedTime.atZone(zoneId).toInstant(),
        zoneId
    )

    private val handler = ValidBetweenFromDateHandler(clock)
    private val writer = mock<DelegatingXMLElementWriter>()

    @Test
    fun shouldReplaceValidBetweenFromDateWithCurrentTime() {
        val attrs: Attributes = mock()
        val oldContent = "1970-01-01T00:00:00".toCharArray()

        handler.startElement(null, "FromDate", "FromDate", attrs, writer)
        handler.characters(oldContent, 0, oldContent.size, writer)
        handler.endElement(null, "FromDate", "FromDate", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(25))

            val transformed = String(firstValue, 0, 25)
            assertThat(transformed)
                .isEqualTo("2020-01-01T12:00:00${offsetString()}")
        }
    }

    private fun offsetString(): String =
        zoneId.rules.getOffset(fixedTime.atZone(zoneId).toInstant()).id
}
