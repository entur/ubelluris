package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.JourneyPatternStopPointCollectorPlugin
import org.xml.sax.Attributes

/**
 * Sets ForAlighting to false on the first, and ForBoarding to false on the last,
 * StopPointInJourneyPattern of each JourneyPattern. Missing elements are not added.
 *
 * Must be registered for StopPointInJourneyPattern and its ForAlighting and ForBoarding children.
 */
class StopPointInJourneyPatternBoardingAlightingHandler(
    private val collectorPlugin: JourneyPatternStopPointCollectorPlugin,
) : XMLElementHandler {
    private val firstStopPointIds by lazy { collectorPlugin.getFirstStopPointIds() }
    private val lastStopPointIds by lazy { collectorPlugin.getLastStopPointIds() }

    private var currentStopPointId: String? = null
    private var overrideText = false

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        when (qName) {
            NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN -> currentStopPointId = attributes?.getValue("id")
            NetexTypes.FOR_ALIGHTING -> overrideText = currentStopPointId in firstStopPointIds
            NetexTypes.FOR_BOARDING -> overrideText = currentStopPointId in lastStopPointIds
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
        if (qName == NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN) {
            currentStopPointId = null
        }
        writer.endElement(uri, localName, qName)
    }

    private companion object {
        val FALSE = "false".toCharArray()
    }
}
