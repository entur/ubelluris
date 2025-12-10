package org.entur.ror.ubelluris.timetable.discovery

import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.model.ScheduledStopPointRef
import org.entur.ror.ubelluris.timetable.model.TimetableData
import org.jdom2.input.SAXBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Extracts ScheduledStopPointRef elements from timetable data
 * Associates each ref with its TransportMode from the parent Line
 */
class ScheduledStopPointExtractor {

    private val logger = LoggerFactory.getLogger(ScheduledStopPointExtractor::class.java)
    private val saxBuilder = SAXBuilder()

    /**
     * Extracts all ScheduledStopPointRefs from timetable data
     *
     * @param timetableDataMap Map of provider to TimetableData
     * @return List of ScheduledStopPointRefs with associated TransportModes
     */
    fun extract(timetableDataMap: Map<String, TimetableData>): List<ScheduledStopPointRef> {
        logger.info("Extracting ScheduledStopPointRefs from timetable data")

        val allRefs = mutableListOf<ScheduledStopPointRef>()

        timetableDataMap.forEach { (provider, timetableData) ->
            logger.info("Processing provider: $provider (${timetableData.modeHelperFiles.size} files)")

            timetableData.modeHelperFiles.forEach { file ->
                try {
                    val refs = extractFromFile(provider, file)
                    allRefs.addAll(refs)
                } catch (e: Exception) {
                    logger.error("Failed to extract from file: $file", e)
                }
            }
        }

        logger.info("Extracted ${allRefs.size} ScheduledStopPointRefs")
        return allRefs
    }

    private fun extractFromFile(provider: String, file: Path): List<ScheduledStopPointRef> {
        val document = saxBuilder.build(file.toFile())
        val root = document.rootElement
        val namespace = root.namespace

        val refs = mutableListOf<ScheduledStopPointRef>()

        // Find all Line elements
        val lines = root.getDescendants(org.jdom2.filter.Filters.element("Line", namespace))

        while (lines.hasNext()) {
            val lineElement = lines.next()

            // Get TransportMode for this Line
            val transportModeElement = lineElement.getChild("TransportMode", namespace)
            val transportModeText = transportModeElement?.text

            if (transportModeText != null) {
                val transportMode = TransportMode.fromNetexValue(transportModeText)

                if (transportMode != null) {
                    // Find all JourneyPatterns within this Line's routes
                    val journeyPatterns = lineElement.getDescendants(
                        org.jdom2.filter.Filters.element("JourneyPattern", namespace)
                    )

                    while (journeyPatterns.hasNext()) {
                        val journeyPattern = journeyPatterns.next()

                        // Find all ScheduledStopPointRef within pointsInSequence
                        val stopPointRefs = journeyPattern.getDescendants(
                            org.jdom2.filter.Filters.element("ScheduledStopPointRef", namespace)
                        )

                        while (stopPointRefs.hasNext()) {
                            val stopPointRef = stopPointRefs.next()
                            val refValue = stopPointRef.getAttributeValue("ref")
                            if (refValue != null) {
                                refs.add(
                                    ScheduledStopPointRef(
                                        provider = provider,
                                        originalRef = refValue,
                                        transportMode = transportMode
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        logger.debug("Extracted ${refs.size} refs from file: ${file.fileName}")
        return refs
    }
}
