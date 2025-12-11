package org.entur.ror.ubelluris.timetable.insertion

import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.timetable.model.Action
import org.entur.ror.ubelluris.timetable.model.ModeInsertionLog
import org.entur.ror.ubelluris.timetable.model.Scenario
import org.entur.ror.ubelluris.timetable.model.StopPlaceAnalysis
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

/**
 * Inserts TransportMode values into stops XML based on analysis
 */
class TransportModeInserter(
    private val stopPlaceSplitter: StopPlaceSplitter
) {

    private val logger = LoggerFactory.getLogger(TransportModeInserter::class.java)
    private val saxBuilder = SAXBuilder()

    fun insert(stopsXmlPath: Path, analyses: List<StopPlaceAnalysis>): Pair<Path, List<ModeInsertionLog>> {
        logger.info("Inserting transport modes for ${analyses.size} StopPlaces")

        val document = saxBuilder.build(stopsXmlPath.toFile())
        val root = document.rootElement
        val namespace = root.namespace

        val logs = mutableListOf<ModeInsertionLog>()

        val singleQuay = analyses.filter { it.scenario == Scenario.SINGLE_QUAY }
        val uniformMode = analyses.filter { it.scenario == Scenario.UNIFORM_MODE }
        val mixedMode = analyses.filter { it.scenario == Scenario.MIXED_MODE }

        logger.info("Scenarios: SINGLE_QUAY=${singleQuay.size}, UNIFORM_MODE=${uniformMode.size}, MIXED_MODE=${mixedMode.size}")

        val stopPlacesIterator = root.getDescendants(Filters.element(NetexTypes.STOP_PLACE, namespace))
        val stopPlaces = mutableListOf<Element>()
        while (stopPlacesIterator.hasNext()) {
            stopPlaces.add(stopPlacesIterator.next())
        }

        stopPlaces.forEach { stopPlaceElement ->
            val stopPlaceId = stopPlaceElement.getAttributeValue("id") ?: return@forEach

            singleQuay.find { it.stopPlaceId == stopPlaceId }?.let { analysis ->
                val newMode = analysis.quayModes.values.first()
                updateStopPlaceMode(stopPlaceElement, namespace, newMode)
                cleanQuayPublicCode(stopPlaceElement, namespace, analysis.quayModes.keys.first())
                logs.add(
                    ModeInsertionLog(
                        stopPlaceId = stopPlaceId,
                        action = Action.UPDATED_SINGLE_QUAY,
                        oldMode = analysis.existingMode,
                        newMode = newMode
                    )
                )
                logger.info("Updated SINGLE_QUAY: $stopPlaceId to $newMode")
            }

            uniformMode.find { it.stopPlaceId == stopPlaceId }?.let { analysis ->
                val newMode = analysis.quayModes.values.first()
                updateStopPlaceMode(stopPlaceElement, namespace, newMode)
                analysis.quayModes.keys.forEach { quayId ->
                    cleanQuayPublicCode(stopPlaceElement, namespace, quayId)
                }
                logs.add(
                    ModeInsertionLog(
                        stopPlaceId = stopPlaceId,
                        action = Action.UPDATED_UNIFORM_MODE,
                        oldMode = analysis.existingMode,
                        newMode = newMode
                    )
                )
                logger.info("Updated UNIFORM_MODE: $stopPlaceId to $newMode")
            }
        }

        if (mixedMode.isNotEmpty()) {
            logger.info("Processing ${mixedMode.size} MIXED_MODE StopPlaces")
            val splitLogs = stopPlaceSplitter.split(document, mixedMode)
            logs.addAll(splitLogs)
        }

        val outputter = XMLOutputter(Format.getPrettyFormat())
        Files.newBufferedWriter(stopsXmlPath).use { writer ->
            outputter.output(document, writer)
        }

        logger.info("Inserted TransportMode values, generated ${logs.size} log entries")
        return Pair(stopsXmlPath, logs)
    }

    private fun updateStopPlaceMode(
        stopPlaceElement: Element,
        namespace: Namespace,
        newMode: org.entur.ror.ubelluris.model.TransportMode
    ) {
        val transportModeElement = stopPlaceElement.getChild("TransportMode", namespace)
        if (transportModeElement != null) {
            transportModeElement.text = newMode.netexValue
        } else {
            val newElement = Element("TransportMode", namespace)
            newElement.text = newMode.netexValue
            val stopPlaceType = stopPlaceElement.getChild("StopPlaceType", namespace)
            if (stopPlaceType != null) {
                val index = stopPlaceElement.indexOf(stopPlaceType)
                stopPlaceElement.addContent(index + 1, newElement)
            } else {
                stopPlaceElement.addContent(0, newElement)
            }
        }
    }

    private fun cleanQuayPublicCode(stopPlaceElement: Element, namespace: Namespace, quayId: String) {
        val quaysContainer = stopPlaceElement.getChild("quays", namespace) ?: return
        val quays = quaysContainer.getChildren(NetexTypes.QUAY, namespace)

        quays.find { it.getAttributeValue("id") == quayId }?.let { quayElement ->
            val publicCodeElement = quayElement.getChild(NetexTypes.PUBLIC_CODE, namespace)
            if (publicCodeElement != null && publicCodeElement.text == "*") {
                publicCodeElement.text = ""
                logger.info("Cleaned PublicCode for Quay: $quayId")
            }
        }
    }
}
