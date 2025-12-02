package org.entur.ror.ubelluris.sax.selectors.entities

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.selections.EntitySelection
import org.entur.netex.tools.lib.selectors.entities.EntitySelector
import org.entur.netex.tools.lib.selectors.entities.EntitySelectorContext
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.PublicCodeRepository

class PublicCodeSelector(val publicCodeRepository: PublicCodeRepository) : EntitySelector {

    override fun selectEntities(context: EntitySelectorContext): EntitySelection {
        val model = context.entityModel
        val activeEntitiesMap = mutableMapOf<String, MutableMap<String, Entity>>()
        val entitiesByTypeAndId = model.getEntitesByTypeAndId()
        entitiesByTypeAndId.forEach { (type, entities) ->
            val entitiesToKeep = when (type) {

                NetexTypes.QUAY -> {
                    entities.filter { entity ->
                        entity.key !in publicCodeRepository.entityIds
                    }
                }

                NetexTypes.STOP_PLACE -> {
                    entities.filter { entity ->
                        val quays = publicCodeRepository.quaysPerStopPlace[entity.key].orEmpty()
                        quays.size != 1 || quays.first().publicCode != "*"
                    }
                }

                else -> entities
            }
            activeEntitiesMap[type] = entitiesToKeep.toMutableMap()
        }

        return EntitySelection(activeEntitiesMap, model)
    }
}