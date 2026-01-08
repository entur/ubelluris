package org.entur.ror.ubelluris.sax.selectors.entities

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.selections.EntitySelection
import org.entur.netex.tools.lib.selectors.entities.EntitySelector
import org.entur.netex.tools.lib.selectors.entities.EntitySelectorContext
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository

class StopPlacePurgingEntitySelector(val stopPlacePurgingRepository: StopPlacePurgingRepository) : EntitySelector {

    override fun selectEntities(context: EntitySelectorContext): EntitySelection {
        val model = context.entityModel
        val activeEntitiesMap = mutableMapOf<String, MutableMap<String, Entity>>()
        val entitiesByTypeAndId = model.getEntitesByTypeAndId()
        val stopPlacesToRemove = mutableSetOf<String>()

        entitiesByTypeAndId.forEach { (type, entities) ->
            val entitiesToKeep = when (type) {

                NetexTypes.QUAY -> {
                    entities.filter { entity ->
                        entity.key !in stopPlacePurgingRepository.entityIds
                    }
                }

                NetexTypes.STOP_PLACE -> {
                    entities.filter { entity ->
                        val quays = stopPlacePurgingRepository.quaysPerStopPlace[entity.key].orEmpty()

                        val remainingQuays = quays.filter { quay ->
                            quay.quayId !in stopPlacePurgingRepository.entityIds
                        }

                        if (stopPlacePurgingRepository.isChildStopPlace(entity.key) && remainingQuays.isEmpty()) {
                            stopPlacesToRemove.add(entity.key)
                            return@filter false
                        }

                        true
                    }
                }

                else -> entities
            }
            activeEntitiesMap[type] = entitiesToKeep.toMutableMap()
        }

        val stopPlaceEntities = activeEntitiesMap[NetexTypes.STOP_PLACE]
        if (stopPlaceEntities != null) {
            val finalStopPlaces = stopPlaceEntities.filter { (stopPlaceId, entity) ->
                val children = stopPlacePurgingRepository.parentSiteRefsPerStopPlace[stopPlaceId]
                if (children != null) {
                    val remainingChildren = children.filter { childId ->
                        childId !in stopPlacesToRemove && stopPlaceEntities.containsKey(childId)
                    }

                    val quays = stopPlacePurgingRepository.quaysPerStopPlace[stopPlaceId].orEmpty()
                    val remainingQuays = quays.filter { quay ->
                        quay.quayId !in stopPlacePurgingRepository.entityIds
                    }

                    if (remainingQuays.isEmpty() && remainingChildren.size <= 1) {
                        return@filter false
                    }
                }
                true
            }
            activeEntitiesMap[NetexTypes.STOP_PLACE] = finalStopPlaces.toMutableMap()
        }

        return EntitySelection(activeEntitiesMap, model)
    }
}