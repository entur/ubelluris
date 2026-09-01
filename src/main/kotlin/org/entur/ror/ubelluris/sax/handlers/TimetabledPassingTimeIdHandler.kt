package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

/**
 * Handler that fixes duplicate TimetabledPassingTime IDs by appending a unique sequence number suffix.
 */
class TimetabledPassingTimeIdHandler : XMLElementHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var sequenceNumber = 0
    private var modificationCount = 0

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        sequenceNumber++

        if (attributes == null || qName != "TimetabledPassingTime") {
            writer.startElement(uri, localName, qName, attributes)
            return
        }

        val id = attributes.getValue("id")
        if (id == null) {
            writer.startElement(uri, localName, qName, attributes)
            return
        }

        // Create new attributes with modified ID
        val newAttributes = AttributesImpl()
        for (i in 0 until attributes.length) {
            val attrName = attributes.getQName(i)
            val attrValue = attributes.getValue(i)

            when (attrName) {
                "id" -> {
                    val newId = "$attrValue-S$sequenceNumber"
                    newAttributes.addAttribute(
                        attributes.getURI(i),
                        attributes.getLocalName(i),
                        attrName,
                        attributes.getType(i),
                        newId,
                    )
                    modificationCount++
                    logger.debug(
                        "Modified TimetabledPassingTime ID from '{}' to '{}' at sequence {}",
                        attrValue,
                        newId,
                        sequenceNumber,
                    )
                }
                else -> {
                    newAttributes.addAttribute(
                        attributes.getURI(i),
                        attributes.getLocalName(i),
                        attrName,
                        attributes.getType(i),
                        attrValue,
                    )
                }
            }
        }

        writer.startElement(uri, localName, qName, newAttributes)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter,
    ) {
        writer.characters(ch, start, length)
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
