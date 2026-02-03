package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.extensions.addNewAttribute
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.handlers.util.AttributeReplacer
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

class SiteFrameHandler(
    private val attributeReplacer: AttributeReplacer
) : XMLElementHandler {
    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter
    ) {
        val idValue = attributeReplacer.replaceAttribute(attributes, "id")
        val newAttributes = AttributesImpl()
        newAttributes.addNewAttribute("id", idValue)
        newAttributes.addNewAttribute("version", "1")
        writer.startElement(uri, NetexTypes.SITE_FRAME, NetexTypes.SITE_FRAME, newAttributes)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter
    ) {
        writer.characters(ch, start, length)
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter
    ) {
        writer.endElement(uri, NetexTypes.SITE_FRAME, NetexTypes.SITE_FRAME)
    }
}