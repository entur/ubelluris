package org.entur.ror.ubelluris.sax.enrichment

import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.QuayModeMapping
import org.entur.ror.ubelluris.model.Scenario
import org.entur.ror.ubelluris.model.StopPlaceAnalysis
import org.entur.ror.ubelluris.model.TransportMode
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Analyzes StopPlaces to determine how to handle TransportMode insertion
 */
class StopPlaceAnalyzer {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val saxBuilder = SAXBuilder()

    fun analyze(
        stopsXmlPath: Path,
        quayModeMapping: QuayModeMapping,
    ): List<StopPlaceAnalysis> {
        logger.info("Analyzing StopPlaces for TransportMode insertion")

        val document = saxBuilder.build(stopsXmlPath.toFile())
        val root = document.rootElement
        val namespace = root.namespace

        val analyses = mutableListOf<StopPlaceAnalysis>()

        val stopPlaces = root.getDescendants(Filters.element(NetexTypes.STOP_PLACE, namespace))

        stopPlaces.forEach { stopPlaceElement ->
            val stopPlaceId = stopPlaceElement.getAttributeValue("id") ?: return@forEach

            val quayIds =
                quayModeMapping.quayToStopPlace
                    .filterValues { it == stopPlaceId }
                    .keys

            if (quayIds.isEmpty()) {
                return@forEach
            }

            val quayModes =
                quayIds
                    .associateWith { quayId ->
                        val modes = quayModeMapping.quayToModes[quayId] ?: return@associateWith null
                        if (modes.size > 1) {
                            logger.warn(
                                "Quay $quayId is associated with several transportModes, using first mode (${modes.first()}) of $modes",
                            )
                        }
                        modes.first()
                    }.filterValues { it != null }
                    .mapValues { it.value!! }

            val totalQuays = countQuays(stopPlaceElement, namespace)

            val scenario = determineScenario(totalQuays, quayModes)

            val existingMode =
                stopPlaceElement
                    .getChildText(NetexTypes.TRANSPORT_MODE, namespace)
                    ?.let { TransportMode.fromNetexValue(it) }
            val existingType = stopPlaceElement.getChildText(NetexTypes.STOP_PLACE_TYPE, namespace)

            val parentSiteRef = stopPlaceElement.getChild(NetexTypes.PARENT_SITE_REF, namespace)
            val parentRef = parentSiteRef?.getAttributeValue("ref")
            val hasParent = parentRef != null

            val analysis =
                StopPlaceAnalysis(
                    stopPlaceId = stopPlaceId,
                    scenario = scenario,
                    quayModes = quayModes,
                    existingMode = existingMode,
                    existingType = existingType,
                    hasParent = hasParent,
                    parentRef = parentRef,
                )

            analyses.add(analysis)
            logger.info("Analyzed StopPlace $stopPlaceId: scenario=$scenario, quays=${quayModes.size}/$totalQuays")
        }

        logger.info("Analyzed ${analyses.size} StopPlaces")
        return analyses
    }

    private fun countQuays(
        stopPlaceElement: Element,
        namespace: Namespace,
    ): Int {
        val quaysContainer = stopPlaceElement.getChild("quays", namespace) ?: return 0
        return quaysContainer.getChildren(NetexTypes.QUAY, namespace).size
    }

    private fun determineScenario(
        totalQuays: Int,
        quayModes: Map<String, TransportMode>,
    ): Scenario =
        when {
            totalQuays == 1 -> Scenario.SINGLE_QUAY
            quayModes.values.distinct().size == 1 && quayModes.size == totalQuays -> Scenario.UNIFORM_MODE
            else -> Scenario.MIXED_MODE
        }
}
