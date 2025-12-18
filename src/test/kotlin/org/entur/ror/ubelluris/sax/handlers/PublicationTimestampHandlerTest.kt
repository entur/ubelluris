package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.xml.sax.Attributes
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PublicationTimestampHandlerTest {

    private val fixedTime = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
    private val clock = Clock.fixed(
        fixedTime.atZone(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault()
    )

    private val handler = PublicationTimestampHandler(clock)
    private val writer = mock<DelegatingXMLElementWriter>()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private val expectedString = fixedTime.format(formatter)

    @Test
    fun shouldReplaceOldPublicationTimestampWithCurrentTime() {
        val attrs: Attributes = mock()
        val oldContent = "2025-11-27T06:00:52".toCharArray()

        handler.startElement(null, "PublicationTimestamp", "PublicationTimestamp", attrs, writer)
        handler.characters(oldContent, 0, oldContent.size, writer)
        handler.endElement(null, "PublicationTimestamp", "PublicationTimestamp", writer)

        argumentCaptor<CharArray>().apply {
            verify(writer).characters(capture(), eq(0), eq(19))

            val transformedContent = String(firstValue, 0, 19)

            assertThat(transformedContent).isEqualTo(expectedString)
        }
    }

}
