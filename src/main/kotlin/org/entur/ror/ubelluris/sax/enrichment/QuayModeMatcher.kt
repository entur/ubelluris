package org.entur.ror.ubelluris.sax.enrichment

import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.QuayModeMapping
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.utils.LogKeys.QUAY_ID
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

    fun match(
        stopsXmlPath: Path,
        localIdToModes: Map<String, Set<TransportMode>>,
    ): QuayModeMapping {
        logger.info("Matching ${localIdToModes.size} local IDs to quays in stops data")
        return matchByLocalIds(stopsXmlPath, localIdToModes)
    }

    private fun matchByLocalIds(
        stopsXmlPath: Path,
        refToModeMap: Map<String, Set<TransportMode>>,
    ): QuayModeMapping {
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

                val keyValues =
                    quayElement
                        .getChild("keyList", namespace)
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
                                logger.info("Matched quay {} to modes: {}", kv(QUAY_ID, quayId), matchedModes)
                            }
                        }
                    }
                }
            }
        }

        logger.info("Matched ${quayToModes.size} quays")

        return QuayModeMapping(
            quayToModes = quayToModes,
            quayToStopPlace = quayToStopPlace,
        )
    }
}
