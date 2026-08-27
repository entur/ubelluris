package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.xml.sax.Attributes

class BookWhenFilterHandler : XMLElementHandler {
    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        writer.startElement(uri, localName, qName, attributes)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter,
    ) {
        if (ch == null) {
            writer.characters(ch, start, length)
            return
        }

        val text = String(ch, start, length).trim()

        if (text == "advanceOnly") {
            val replacement = "advanceAndDayOfTravel".toCharArray()
            writer.characters(replacement, 0, replacement.size)
        } else {
            writer.characters(ch, start, length)
        }
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter,
    ) {
        writer.endElement(uri, localName, qName)
    }
}
