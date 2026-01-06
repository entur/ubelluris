package org.entur.ror.ubelluris.timetable.insertion

import org.entur.ror.ubelluris.model.NetexTypes
import org.jdom2.Element
import org.jdom2.Namespace
import org.slf4j.LoggerFactory

class StopPlaceParentCreator {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun createParentStopPlace(
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
}