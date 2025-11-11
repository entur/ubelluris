package plugin.handlers

import model.NetexTypes
import org.entur.netex.tools.lib.extensions.addNewAttribute
import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

class StopPlaceIdHandler(
) : XMLElementHandler {
    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter
    ) {
        val idValue = attributes?.getValue("id")?.replace("SE:050", "SAM") ?: ""
        val newAttributes = AttributesImpl()
        newAttributes.addNewAttribute("id", idValue)
        newAttributes.addNewAttribute("version", "1")
        writer.startElement(uri, NetexTypes.STOP_PLACE, NetexTypes.STOP_PLACE, newAttributes)
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
        writer.endElement(uri, NetexTypes.STOP_PLACE, NetexTypes.STOP_PLACE)
    }
}