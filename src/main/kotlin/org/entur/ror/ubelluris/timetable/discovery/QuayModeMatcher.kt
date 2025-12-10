package org.entur.ror.ubelluris.timetable.discovery

import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.model.*
import org.jdom2.input.SAXBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Matches ScheduledStopPointRefs from timetables to Quays in stops data
 * Uses local-stoppoint-gid KeyValue for matching
 */
class QuayModeMatcher {

    private val logger = LoggerFactory.getLogger(QuayModeMatcher::class.java)
    private val saxBuilder = SAXBuilder()

    /**
     * Matches timetable refs to quays in stops XML
     *
     * @param stopsXmlPath Path to stops XML file
     * @param scheduledStopPointRefs List of refs extracted from timetables
     * @return QuayModeMapping with matched quays and detected issues
     */
    fun match(stopsXmlPath: Path, scheduledStopPointRefs: List<ScheduledStopPointRef>): QuayModeMapping {
        logger.info("Matching ${scheduledStopPointRefs.size} ScheduledStopPointRefs to quays in stops data")

        // Build index of codespace-formatted refs from timetables
        val refToModeMap = buildRefToModeMap(scheduledStopPointRefs)
        logger.info("Built index of ${refToModeMap.size} unique refs")

        // Parse stops XML and match quays
        val document = saxBuilder.build(stopsXmlPath.toFile())
        val root = document.rootElement
        val namespace = root.namespace

        val quayToModes = mutableMapOf<String, MutableSet<TransportMode>>()
        val quayToStopPlace = mutableMapOf<String, String>()
        val issues = mutableListOf<MatchIssue>()

        // Find all StopPlace elements
        val stopPlaces = root.getDescendants(org.jdom2.filter.Filters.element(NetexTypes.STOP_PLACE, namespace))

        stopPlaces.forEach { stopPlaceElement ->
            val stopPlaceId = stopPlaceElement.getAttributeValue("id") ?: return@forEach

            // Find all Quays
            val quays = stopPlaceElement.getDescendants(org.jdom2.filter.Filters.element(NetexTypes.QUAY, namespace))

            quays.forEach { quayElement ->
                val quayId = quayElement.getAttributeValue("id") ?: return@forEach

                // Find local-stoppoint-gid KeyValue
                val keyValues = quayElement.getChild("keyList", namespace)
                    ?.getChildren(NetexTypes.KEY_VALUE, namespace)
                    ?: emptyList()

                keyValues.forEach { keyValue ->
                    val key = keyValue.getChildText(NetexTypes.KEY, namespace)
                    if (key == "local-stoppoint-gid") {
                        val value = keyValue.getChildText(NetexTypes.VALUE, namespace)
                        if (value != null) {
                            // Parse pipe-separated values
                            val gids = value.split("|")
                            val matchedModes = mutableSetOf<TransportMode>()

                            gids.forEach { gid ->
                                // Check if this gid matches any timetable ref
                                val modes = refToModeMap[gid.trim()]
                                if (modes != null) {
                                    matchedModes.addAll(modes)
                                }
                            }

                            if (matchedModes.isNotEmpty()) {
                                quayToModes.getOrPut(quayId) { mutableSetOf() }.addAll(matchedModes)
                                quayToStopPlace[quayId] = stopPlaceId
                                logger.debug("Matched quay $quayId to modes: $matchedModes")
                            }
                        }
                    }
                }
            }
        }

        // Detect missing matches (refs not found in stops)
        val matchedRefs = quayToModes.values.flatten().toSet()
        refToModeMap.forEach { (ref, modes) ->
            if (modes.none { it in matchedRefs }) {
                issues.add(
                    MatchIssue(
                        type = IssueType.MISSING_MATCH,
                        message = "ScheduledStopPointRef not found in stops data",
                        scheduledStopPointRef = ref
                    )
                )
            }
        }

        logger.info("Matched ${quayToModes.size} quays, detected ${issues.size} issues")

        return QuayModeMapping(
            quayToModes = quayToModes,
            quayToStopPlace = quayToStopPlace,
            issues = issues
        )
    }

    /**
     * Builds a map from codespace-formatted ref (e.g., "014:123456") to TransportModes
     */
    private fun buildRefToModeMap(refs: List<ScheduledStopPointRef>): Map<String, Set<TransportMode>> {
        val map = mutableMapOf<String, MutableSet<TransportMode>>()

        refs.forEach { ref ->
            val codespaceFormat = ref.toCodespaceFormat()
            if (codespaceFormat != null) {
                map.getOrPut(codespaceFormat) { mutableSetOf() }.add(ref.transportMode)
            } else {
                logger.warn("Failed to convert ref to codespace format: ${ref.originalRef}")
            }
        }

        return map
    }
}
