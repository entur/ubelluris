package org.entur.ror.ubelluris.sax.plugins

import org.entur.ror.ubelluris.sax.plugins.data.QuayData

class StopPlacePurgingRepository(
    val entityIds: MutableList<String> = mutableListOf(),
    val quaysPerStopPlace: MutableMap<String, MutableSet<QuayData>> = mutableMapOf(),
    val parentSiteRefsPerStopPlace: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    val childStopPlaces: MutableSet<String> = mutableSetOf(),
    val illegalPublicCodes: List<String> = listOf("*", "-")
) {

    fun addEntityId(entityId: String) {
        entityIds.add(entityId)
    }

    fun addQuayToStopPlace(stopPlaceId: String, quayData: QuayData) {
        quaysPerStopPlace.getOrPut(stopPlaceId) { mutableSetOf() }.add(quayData)
    }

    fun addChildStopToParent(parentSiteRef: String, childId: String) {
        parentSiteRefsPerStopPlace.getOrPut(parentSiteRef) { mutableSetOf() }.add(childId)
        childStopPlaces.add(childId)
    }

    fun isChildStopPlace(stopPlaceId: String): Boolean {
        return stopPlaceId in childStopPlaces
    }
}