package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

/**
 * Removes empty <members/> elements (e.g. in GroupOfStopPlaces), which are invalid according to the NeTEx schema.
 *
 * This single handler instance must be registered for the members element and all its child elements.
 * The start tag of members is buffered and only written when the first child element is encountered.
 */
class EmptyMembersFilterHandler(
    private val membersElementName: String = "members",
) : XMLElementHandler {
    private var pendingStart: PendingStart? = null

    private class PendingStart(
        val uri: String?,
        val localName: String?,
        val qName: String?,
        val attributes: Attributes,
    )

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        if (qName == membersElementName) {
            pendingStart = PendingStart(uri, localName, qName, AttributesImpl(attributes ?: AttributesImpl()))
            return
        }
        flushPendingStart(writer)
        writer.startElement(uri, localName, qName, attributes)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter,
    ) {
        // Whitespace inside a not yet written members element is dropped
        if (pendingStart == null) {
            writer.characters(ch, start, length)
        }
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter,
    ) {
        if (qName == membersElementName && pendingStart != null) {
            pendingStart = null
            return
        }
        writer.endElement(uri, localName, qName)
    }

    private fun flushPendingStart(writer: DelegatingXMLElementWriter) {
        pendingStart?.let {
            writer.startElement(it.uri, it.localName, it.qName, it.attributes)
            pendingStart = null
        }
    }
}
