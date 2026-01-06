package org.entur.ror.ubelluris.timetable.insertion

import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.TransportMode
import org.jdom2.Element
import org.jdom2.Namespace

class StopPlaceChildCreator {

    fun createChildStopPlace(
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

        val transportModeElement = childStopPlace.getChild(NetexTypes.TRANSPORT_MODE, namespace)
            ?: Element(NetexTypes.TRANSPORT_MODE, namespace).also { childStopPlace.addContent(0, it) }
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
}