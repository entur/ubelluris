package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.xml.sax.Attributes
import java.math.RoundingMode

class CoordinateRoundingHandler : XMLElementHandler {
    private val buffer = StringBuilder()

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        buffer.setLength(0)
        writer.startElement(uri, localName, qName, attributes)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter,
    ) {
        if (ch != null) {
            buffer.appendRange(ch, start, start + length)
        }
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter,
    ) {
        val text = round(buffer.toString())
        if (text.isNotEmpty()) {
            writer.characters(text.toCharArray(), 0, text.length)
        }
        buffer.setLength(0)
        writer.endElement(uri, localName, qName)
    }

    private fun round(text: String): String {
        val trimmed = text.trim()
        val value = trimmed.toBigDecimalOrNull() ?: return text
        if (value.scale() <= SCALE) {
            return trimmed
        }
        // same as rounding used in Tiamat
        return value.setScale(SCALE, RoundingMode.HALF_UP).toPlainString()
    }

    companion object {
        private const val SCALE = 6
    }
}
