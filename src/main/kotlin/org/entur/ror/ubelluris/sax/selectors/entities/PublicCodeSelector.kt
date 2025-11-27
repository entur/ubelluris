package org.entur.ror.ubelluris.sax.selectors.entities

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.selections.EntitySelection
import org.entur.netex.tools.lib.selectors.entities.EntitySelector
import org.entur.netex.tools.lib.selectors.entities.EntitySelectorContext
import org.entur.ror.ubelluris.sax.plugins.PublicCodeRepository

class PublicCodeSelector(val publicCodeRepository: PublicCodeRepository) : EntitySelector {
    override fun selectEntities(context: EntitySelectorContext): EntitySelection {
        val model = context.entityModel
        val activeEntitiesMap = mutableMapOf<String, MutableMap<String, Entity>>()
        val entitiesByTypeAndId = model.getEntitesByTypeAndId()
        entitiesByTypeAndId.forEach { (type, entities) ->
            if (type == "Quay") {
                val entitiesToKeep = entities.filter { entity -> entity.key !in publicCodeRepository.entityIds }
                if (entitiesToKeep.isNotEmpty()) {
                    activeEntitiesMap.put(type, entitiesToKeep.toMutableMap())
                }

            } else {
                activeEntitiesMap.put(type, entities.toMutableMap())
            }

        }

        return EntitySelection(activeEntitiesMap, model)
    }
}