package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.xml.sax.Attributes
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PublicationTimestampHandler(
    private val clock: Clock = Clock.systemDefaultZone()
) : XMLElementHandler {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter
    ) {
        writer.startElement(uri, localName, qName, attributes)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter
    ) {
        val timestamp = LocalDateTime.now(clock).format(formatter)
        writer.characters(timestamp.toCharArray(), 0, timestamp.length)
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter
    ) {
        writer.endElement(uri, localName, qName)
    }
}
