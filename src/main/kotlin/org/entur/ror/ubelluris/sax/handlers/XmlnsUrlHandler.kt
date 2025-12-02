package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.model.NetexTypes
import org.xml.sax.Attributes

class XmlnsUrlHandler(val replacementUrl: String = "http://www.samtrafiken.se/ns/sam") : XMLElementHandler {

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter
    ) {
        writer.startElement(uri, NetexTypes.XML_NS_URL, NetexTypes.XML_NS_URL, attributes)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter
    ) {

    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter
    ) {
        val charArray = replacementUrl.toCharArray()
        writer.characters(charArray, 0, charArray.size)
        writer.endElement(uri, NetexTypes.XML_NS_URL, NetexTypes.XML_NS_URL)
    }
}