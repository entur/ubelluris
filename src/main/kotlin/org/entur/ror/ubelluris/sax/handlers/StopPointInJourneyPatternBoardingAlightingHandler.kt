package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.JourneyPatternStopPointCollectorPlugin
import org.entur.ror.ubelluris.sax.plugins.data.StopPointInJourneyPatternKey
import org.xml.sax.Attributes

/**
 * Sets ForAlighting to false on the first, and ForBoarding to false on the last,
 * StopPointInJourneyPattern of each JourneyPattern. Missing elements are not added.
 *
 * Must be registered for JourneyPattern, StopPointInJourneyPattern and its ForAlighting and ForBoarding children.
 */
class StopPointInJourneyPatternBoardingAlightingHandler(
    private val collectorPlugin: JourneyPatternStopPointCollectorPlugin,
) : XMLElementHandler {
    private val firstStopPoints by lazy { collectorPlugin.getFirstStopPoints() }
    private val lastStopPoints by lazy { collectorPlugin.getLastStopPoints() }

    private var currentJourneyPatternId: String? = null
    private var currentStopPoint: StopPointInJourneyPatternKey? = null
    private var overrideText = false

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        when (qName) {
            NetexTypes.JOURNEY_PATTERN -> currentJourneyPatternId = attributes?.getValue("id")
            NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN -> currentStopPoint = stopPointKey(attributes)
            NetexTypes.FOR_ALIGHTING -> overrideText = currentStopPoint in firstStopPoints
            NetexTypes.FOR_BOARDING -> overrideText = currentStopPoint in lastStopPoints
        }
        writer.startElement(uri, localName, qName, attributes)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter,
    ) {
        if (!overrideText) {
            writer.characters(ch, start, length)
        }
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter,
    ) {
        if (overrideText) {
            writer.characters(FALSE, 0, FALSE.size)
            overrideText = false
        }
        when (qName) {
            NetexTypes.JOURNEY_PATTERN -> currentJourneyPatternId = null
            NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN -> currentStopPoint = null
        }
        writer.endElement(uri, localName, qName)
    }

    private fun stopPointKey(attributes: Attributes?): StopPointInJourneyPatternKey? {
        val journeyPatternId = currentJourneyPatternId ?: return null
        val id = attributes?.getValue("id") ?: return null
        return StopPointInJourneyPatternKey(journeyPatternId, id, attributes.getValue("version"), attributes.getValue("order"))
    }

    private companion object {
        val FALSE = "false".toCharArray()
    }
}
