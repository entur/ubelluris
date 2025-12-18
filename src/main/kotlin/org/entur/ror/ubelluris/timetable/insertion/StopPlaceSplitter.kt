package org.entur.ror.ubelluris.timetable.insertion

import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.timetable.model.StopPlaceAnalysis
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.filter.Filters
import org.slf4j.LoggerFactory

/**
 * Handles splitting of MIXED_MODE StopPlaces into multiple child StopPlaces
 */
class StopPlaceSplitter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun split(document: Document, mixedModeAnalyses: List<StopPlaceAnalysis>) {
        logger.info("Splitting ${mixedModeAnalyses.size} MIXED_MODE StopPlaces")

        val root = document.rootElement
        val namespace = root.namespace

        val stopPlacesContainer = root.getDescendants(Filters.element("stopPlaces", namespace))
            .firstOrNull() ?: run {
            logger.error("Could not find stopPlaces container")
            return
        }

        mixedModeAnalyses.forEach { analysis ->
            val stopPlaceElement = findStopPlaceById(stopPlacesContainer, namespace, analysis.stopPlaceId)
            if (stopPlaceElement == null) {
                logger.error("Could not find StopPlace: ${analysis.stopPlaceId}")
                return@forEach
            }

            val childStopPlaces = performSplit(stopPlaceElement, namespace, analysis)
            logger.info("Split StopPlace ${analysis.stopPlaceId} into ${childStopPlaces.size} children")
        }

        logger.info("Split complete for ${mixedModeAnalyses.size} StopPlaces")
    }

    private fun performSplit(
        originalStopPlace: Element,
        namespace: Namespace,
        analysis: StopPlaceAnalysis
    ): List<String> {
        val originalId = analysis.stopPlaceId
        val quaysContainer = originalStopPlace.getChild("quays", namespace)
            ?: throw IllegalStateException("StopPlace has no quays container: $originalId")

        val quaysByMode = analysis.quayModes.entries.groupBy({ it.value }, { it.key })

        val childStopPlaceIds = mutableListOf<String>()
        val parentId = if (!analysis.hasParent) generateParentId(originalId) else analysis.parentRef

        quaysByMode.forEach { (mode, quayIds) ->
            val childId = generateChildId(originalId, mode)
            childStopPlaceIds.add(childId)

            val childStopPlace = createChildStopPlace(
                originalStopPlace = originalStopPlace,
                childId = childId,
                mode = mode,
                quayIds = quayIds,
                namespace = namespace,
                parentRef = parentId!!
            )

            val parent = originalStopPlace.parentElement
            val index = parent.indexOf(originalStopPlace)
            parent.addContent(index, childStopPlace)
        }

        if (!analysis.hasParent) {
            val parent = createParentStopPlace(originalStopPlace, namespace, parentId!!)
            val parentContainer = originalStopPlace.parentElement
            val index = parentContainer.indexOf(originalStopPlace)
            parentContainer.addContent(index, parent)

            addParentSiteRefToStopPlace(originalStopPlace, namespace, parentId)
        }

        val allQuays = quaysContainer.getChildren(NetexTypes.QUAY, namespace).toList()
        allQuays.forEach { quayElement ->
            val quayId = quayElement.getAttributeValue("id")
            if (quayId in analysis.quayModes.keys) {
                quaysContainer.removeContent(quayElement)
            }
        }

        return childStopPlaceIds
    }

    private fun createChildStopPlace(
        originalStopPlace: Element,
        childId: String,
        mode: TransportMode,
        quayIds: List<String>,
        namespace: Namespace,
        parentRef: String
    ): Element {
        val childStopPlace = originalStopPlace.clone()
        childStopPlace.setAttribute("id", childId)
        childStopPlace.setAttribute("version", "1")

        val transportModeElement = childStopPlace.getChild("TransportMode", namespace)
            ?: Element("TransportMode", namespace).also { childStopPlace.addContent(0, it) }
        transportModeElement.text = mode.netexValue

        val parentSiteRef = childStopPlace.getChild(NetexTypes.PARENT_SITE_REF, namespace)
            ?: Element(NetexTypes.PARENT_SITE_REF, namespace).also {
                val modeIndex = childStopPlace.indexOf(transportModeElement)
                childStopPlace.addContent(modeIndex + 1, it)
            }
        parentSiteRef.setAttribute("ref", parentRef)
        parentSiteRef.setAttribute("version", "1")

        val childQuaysContainer = childStopPlace.getChild("quays", namespace)
        if (childQuaysContainer != null) {
            val allQuays = childQuaysContainer.getChildren(NetexTypes.QUAY, namespace).toList()
            allQuays.forEach { quayElement ->
                val quayId = quayElement.getAttributeValue("id")
                if (quayId !in quayIds) {
                    childQuaysContainer.removeContent(quayElement)
                } else {
                    val publicCodeElement = quayElement.getChild(NetexTypes.PUBLIC_CODE, namespace)
                    if (publicCodeElement?.text == "*") {
                        publicCodeElement.text = ""
                    }
                }
            }
        }

        return childStopPlace
    }

    private fun createParentStopPlace(
        originalStopPlace: Element,
        namespace: Namespace,
        parentId: String
    ): Element {
        val parent = Element(NetexTypes.STOP_PLACE, namespace)
        parent.setAttribute("id", parentId)
        parent.setAttribute("version", "1")

        val keyList = Element("keyList", namespace)
        parent.addContent(keyList)

        val ownerKv = originalStopPlace.getChild("keyList", namespace)
            ?.getChildren(NetexTypes.KEY_VALUE, namespace)
            ?.find { it.getChildText(NetexTypes.KEY, namespace) == "owner" }
        val dataFromKv = originalStopPlace.getChild("keyList", namespace)
            ?.getChildren(NetexTypes.KEY_VALUE, namespace)
            ?.find { it.getChildText(NetexTypes.KEY, namespace) == "data-from" }

        if (ownerKv != null) {
            keyList.addContent(ownerKv.clone())
        }
        if (dataFromKv != null) {
            keyList.addContent(dataFromKv.clone())
        }

        val nameElement = originalStopPlace.getChild("Name", namespace)
        if (nameElement != null) {
            parent.addContent(nameElement.clone())
        }

        val centroid = originalStopPlace.getChild("Centroid", namespace)
        if (centroid != null) {
            parent.addContent(centroid.clone())
        }

        logger.info("Created parent StopPlace $parentId")
        return parent
    }

    private fun addParentSiteRefToStopPlace(
        stopPlace: Element,
        namespace: Namespace,
        parentId: String
    ) {
        stopPlace.getChild(NetexTypes.PARENT_SITE_REF, namespace)?.let {
            stopPlace.removeContent(it)
        }

        val transportMode = stopPlace.getChild("TransportMode", namespace)
        val insertIndex = if (transportMode != null) {
            stopPlace.indexOf(transportMode) + 1
        } else {
            0
        }

        val parentSiteRef = Element(NetexTypes.PARENT_SITE_REF, namespace)
        parentSiteRef.setAttribute("ref", parentId)
        parentSiteRef.setAttribute("version", "1")

        stopPlace.addContent(insertIndex, parentSiteRef)
    }

    private fun findStopPlaceById(container: Element, namespace: Namespace, stopPlaceId: String): Element? {
        return container.getChildren(NetexTypes.STOP_PLACE, namespace)
            .find { it.getAttributeValue("id") == stopPlaceId }
    }

    private fun generateParentId(originalId: String): String {
        val baseId = originalId.substringAfterLast(":")
        val codespace = originalId.substringBeforeLast(":")
        return "$codespace:0000000_${baseId}_parent"
    }

    private fun generateChildId(originalId: String, mode: TransportMode): String {
        val baseId = originalId.substringAfterLast(":")
        val codespace = originalId.substringBeforeLast(":")
        return "$codespace:0000000_${baseId}_${mode.netexValue}"
    }
}
