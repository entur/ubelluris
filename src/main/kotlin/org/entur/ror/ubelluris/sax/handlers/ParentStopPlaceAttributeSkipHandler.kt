package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.xml.sax.Attributes

/**
 * Skips TransportMode, StopPlaceType, and Weighting for parent StopPlaces
 */
class ParentStopPlaceAttributeSkipHandler(
    private val repository: StopPlacePurgingRepository
) : XMLElementHandler {

    private var currentStopPlaceId: String? = null
    private var isInsideParentStopPlace = false

    fun notifyStopPlaceStart(stopPlaceId: String) {
        currentStopPlaceId = stopPlaceId
        val hasChildren = repository.parentSiteRefsPerStopPlace[stopPlaceId]?.isNotEmpty() == true
        val hasQuays = repository.quaysPerStopPlace[stopPlaceId]?.isNotEmpty() == true
        isInsideParentStopPlace = hasChildren && !hasQuays
    }

    fun notifyStopPlaceEnd() {
        currentStopPlaceId = null
        isInsideParentStopPlace = false
    }

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter
    ) {
        if (!isInsideParentStopPlace) {
            writer.startElement(uri, localName, qName, attributes)
        }
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter
    ) {
        if (!isInsideParentStopPlace) {
            writer.characters(ch, start, length)
        }
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter
    ) {
        if (!isInsideParentStopPlace) {
            writer.endElement(uri, localName, qName)
        }
    }
}
