package org.entur.ror.ubelluris.timetable.insertion

import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.model.Action
import org.entur.ror.ubelluris.timetable.model.ModeInsertionLog
import org.entur.ror.ubelluris.timetable.model.StopPlaceAnalysis
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.slf4j.LoggerFactory

/**
 * Handles splitting of MIXED_MODE StopPlaces into multiple child StopPlaces
 */
class StopPlaceSplitter {

    private val logger = LoggerFactory.getLogger(StopPlaceSplitter::class.java)

    /**
     * Splits MIXED_MODE StopPlaces in the document
     *
     * @param document JDOM2 Document containing stops XML
     * @param mixedModeAnalyses List of StopPlaceAnalysis for MIXED_MODE scenarios
     * @return List of ModeInsertionLog entries for split operations
     */
    fun split(document: Document, mixedModeAnalyses: List<StopPlaceAnalysis>): List<ModeInsertionLog> {
        logger.info("Splitting ${mixedModeAnalyses.size} MIXED_MODE StopPlaces")

        val root = document.rootElement
        val namespace = root.namespace
        val logs = mutableListOf<ModeInsertionLog>()

        // Find stopPlaces container
        val stopPlacesContainer = root.getDescendants(org.jdom2.filter.Filters.element("stopPlaces", namespace))
            .firstOrNull() ?: run {
            logger.error("Could not find stopPlaces container")
            return emptyList()
        }

        mixedModeAnalyses.forEach { analysis ->
            val stopPlaceElement = findStopPlaceById(stopPlacesContainer, namespace, analysis.stopPlaceId)
            if (stopPlaceElement == null) {
                logger.error("Could not find StopPlace: ${analysis.stopPlaceId}")
                return@forEach
            }

            val childStopPlaces = performSplit(stopPlaceElement, namespace, analysis)
            logs.add(
                ModeInsertionLog(
                    stopPlaceId = analysis.stopPlaceId,
                    action = Action.SPLIT_MIXED_MODE,
                    oldMode = analysis.existingMode,
                    newMode = null,
                    childStopPlaces = childStopPlaces
                )
            )
            logger.debug("Split StopPlace ${analysis.stopPlaceId} into ${childStopPlaces.size} children")
        }

        logger.info("Split complete, generated ${logs.size} log entries")
        return logs
    }

    private fun performSplit(
        originalStopPlace: Element,
        namespace: Namespace,
        analysis: StopPlaceAnalysis
    ): List<String> {
        val originalId = analysis.stopPlaceId
        val quaysContainer = originalStopPlace.getChild("quays", namespace)
            ?: throw IllegalStateException("StopPlace has no quays container: $originalId")

        // Group quays by mode
        val quaysByMode = analysis.quayModes.entries.groupBy({ it.value }, { it.key })

        val childStopPlaceIds = mutableListOf<String>()

        // Create child StopPlace for each mode
        quaysByMode.forEach { (mode, quayIds) ->
            val childId = generateChildId(originalId, mode)
            childStopPlaceIds.add(childId)

            val childStopPlace = createChildStopPlace(
                originalStopPlace = originalStopPlace,
                childId = childId,
                mode = mode,
                quayIds = quayIds,
                quaysContainer = quaysContainer,
                namespace = namespace,
                parentRef = originalId
            )

            // Insert child into document (before the original)
            val parent = originalStopPlace.parentElement
            val index = parent.indexOf(originalStopPlace)
            parent.addContent(index, childStopPlace)
        }

        // Convert original to parent StopPlace
        if (!analysis.hasParent) {
            convertToParent(originalStopPlace, namespace)
        } else {
            // Original has a parent, so it becomes a child too
            // Keep it but add to parent ref
            val remainingQuays = quaysContainer.getChildren(NetexTypes.QUAY, namespace)
                .filter { quayElement ->
                    val quayId = quayElement.getAttributeValue("id")
                    quayId !in analysis.quayModes.keys
                }

            if (remainingQuays.isEmpty()) {
                // Remove original if all quays were moved to children
                originalStopPlace.parentElement.removeContent(originalStopPlace)
            }
        }

        return childStopPlaceIds
    }

    private fun createChildStopPlace(
        originalStopPlace: Element,
        childId: String,
        mode: TransportMode,
        quayIds: List<String>,
        quaysContainer: Element,
        namespace: Namespace,
        parentRef: String
    ): Element {
        // Clone original StopPlace
        val childStopPlace = originalStopPlace.clone()
        childStopPlace.setAttribute("id", childId)
        childStopPlace.setAttribute("version", "1")

        // Set TransportMode
        val transportModeElement = childStopPlace.getChild("TransportMode", namespace)
            ?: Element("TransportMode", namespace).also { childStopPlace.addContent(0, it) }
        transportModeElement.text = mode.netexValue

        // Set ParentSiteRef
        val parentSiteRef = childStopPlace.getChild(NetexTypes.PARENT_SITE_REF, namespace)
            ?: Element(NetexTypes.PARENT_SITE_REF, namespace).also {
                // Insert after TransportMode
                val modeIndex = childStopPlace.indexOf(transportModeElement)
                childStopPlace.addContent(modeIndex + 1, it)
            }
        parentSiteRef.setAttribute("ref", parentRef)
        parentSiteRef.setAttribute("version", "1")

        // Filter quays - keep only those for this mode
        val childQuaysContainer = childStopPlace.getChild("quays", namespace)
        if (childQuaysContainer != null) {
            val allQuays = childQuaysContainer.getChildren(NetexTypes.QUAY, namespace).toList()
            allQuays.forEach { quayElement ->
                val quayId = quayElement.getAttributeValue("id")
                if (quayId !in quayIds) {
                    childQuaysContainer.removeContent(quayElement)
                } else {
                    // Clean PublicCode if it's "*"
                    val publicCodeElement = quayElement.getChild(NetexTypes.PUBLIC_CODE, namespace)
                    if (publicCodeElement?.text == "*") {
                        publicCodeElement.text = ""
                    }
                }
            }
        }

        return childStopPlace
    }

    private fun convertToParent(stopPlaceElement: Element, namespace: Namespace) {
        // Remove all quays
        val quaysContainer = stopPlaceElement.getChild("quays", namespace)
        quaysContainer?.let { stopPlaceElement.removeContent(it) }

        // Remove TransportMode, StopPlaceType, Weighting
        stopPlaceElement.getChild("TransportMode", namespace)?.let { stopPlaceElement.removeContent(it) }
        stopPlaceElement.getChild("StopPlaceType", namespace)?.let { stopPlaceElement.removeContent(it) }
        stopPlaceElement.getChild("Weighting", namespace)?.let { stopPlaceElement.removeContent(it) }

        logger.debug("Converted StopPlace ${stopPlaceElement.getAttributeValue("id")} to parent")
    }

    private fun findStopPlaceById(container: Element, namespace: Namespace, stopPlaceId: String): Element? {
        return container.getChildren(NetexTypes.STOP_PLACE, namespace)
            .find { it.getAttributeValue("id") == stopPlaceId }
    }

    private fun generateChildId(originalId: String, mode: TransportMode): String {
        val baseId = originalId.substringAfterLast(":")
        val codespace = originalId.substringBeforeLast(":")
        return "$codespace:0000000_${baseId}_${mode.netexValue}"
    }
}
