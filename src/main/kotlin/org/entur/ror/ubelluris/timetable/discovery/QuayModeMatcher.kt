package org.entur.ror.ubelluris.timetable.discovery

import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.model.QuayModeMapping
import org.entur.ror.ubelluris.timetable.model.ScheduledStopPointRef
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Matches ScheduledStopPointRefs from timetables to Quays in stops data
 * Uses local-stoppoint-gid KeyValue for matching
 */
class QuayModeMatcher {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val saxBuilder = SAXBuilder()

    fun match(stopsXmlPath: Path, scheduledStopPointRefs: List<ScheduledStopPointRef>): QuayModeMapping {
        logger.info("Matching ${scheduledStopPointRefs.size} ScheduledStopPointRefs to quays in stops data")

        val refToModeMap = buildRefToModeMap(scheduledStopPointRefs)
        logger.info("Built index of ${refToModeMap.size} unique refs")

        val document = saxBuilder.build(stopsXmlPath.toFile())
        val root = document.rootElement
        val namespace = root.namespace

        val quayToModes = mutableMapOf<String, MutableSet<TransportMode>>()
        val quayToStopPlace = mutableMapOf<String, String>()

        val stopPlaces = root.getDescendants(Filters.element(NetexTypes.STOP_PLACE, namespace))

        stopPlaces.forEach { stopPlaceElement ->
            val stopPlaceId = stopPlaceElement.getAttributeValue("id") ?: return@forEach

            val quays = stopPlaceElement.getDescendants(Filters.element(NetexTypes.QUAY, namespace))

            quays.forEach { quayElement ->
                val quayId = quayElement.getAttributeValue("id") ?: return@forEach

                val keyValues = quayElement.getChild("keyList", namespace)
                    ?.getChildren(NetexTypes.KEY_VALUE, namespace)
                    ?: emptyList()

                keyValues.forEach { keyValue ->
                    val key = keyValue.getChildText(NetexTypes.KEY, namespace)
                    if (key == "local-stoppoint-gid") {
                        val value = keyValue.getChildText(NetexTypes.VALUE, namespace)
                        if (value != null) {
                            val gids = value.split("|")
                            val matchedModes = mutableSetOf<TransportMode>()

                            gids.forEach { gid ->
                                val modes = refToModeMap[gid.trim()]
                                if (modes != null) {
                                    matchedModes.addAll(modes)
                                }
                            }

                            if (matchedModes.isNotEmpty()) {
                                quayToModes.getOrPut(quayId) { mutableSetOf() }.addAll(matchedModes)
                                quayToStopPlace[quayId] = stopPlaceId
                                logger.info("Matched quay $quayId to modes: $matchedModes")
                            }
                        }
                    }
                }
            }
        }

        logger.info("Matched ${quayToModes.size} quays")

        return QuayModeMapping(
            quayToModes = quayToModes,
            quayToStopPlace = quayToStopPlace
        )
    }

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
