package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.model.NetexTypes
import org.xml.sax.Attributes

class XmlnsHandler(val sourceCodespace: String, val targetCodespace: String) : XMLElementHandler {
    private val contentBuffer = StringBuilder()

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter
    ) {
        contentBuffer.clear()
        writer.startElement(uri, NetexTypes.XML_NS, NetexTypes.XML_NS, attributes)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter
    ) {
        if (ch != null) {
            contentBuffer.append(ch, start, length)
        }
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter
    ) {
        val transformedContent = contentBuffer.toString().replace(sourceCodespace, targetCodespace)
        val charArray = transformedContent.toCharArray()
        writer.characters(charArray, 0, charArray.size)
        writer.endElement(uri, NetexTypes.XML_NS, NetexTypes.XML_NS)
    }
}