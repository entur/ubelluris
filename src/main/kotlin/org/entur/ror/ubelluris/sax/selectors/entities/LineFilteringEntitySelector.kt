package org.entur.ror.ubelluris.sax.selectors.entities

import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.selections.EntitySelection
import org.entur.netex.tools.lib.selectors.entities.EntitySelector
import org.entur.netex.tools.lib.selectors.entities.EntitySelectorContext
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.LineFilteringRepository
import org.slf4j.LoggerFactory

/**
 * Entity selector that removes Line entities based on the LineFilteringRepository.
 * Lines that are marked for removal (typically due to their PublicCode matching
 * a regex pattern) are excluded from the output.
 *
 * When used with reference pruning enabled, this will automatically cause dependent
 * entities (Routes, JourneyPatterns, ServiceJourneys, etc.) to be removed as well.
 *
 * @property lineFilteringRepository Repository containing Line IDs to remove
 */
class LineFilteringEntitySelector(
    private val lineFilteringRepository: LineFilteringRepository,
) : EntitySelector {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun selectEntities(context: EntitySelectorContext): EntitySelection {
        logger.info(
            "LineFilteringEntitySelector invoked",
            kv("lineIdsToRemove", lineFilteringRepository.lineIdsToRemove),
        )

        val model = context.entityModel
        val activeEntitiesMap = mutableMapOf<String, MutableMap<String, Entity>>()
        val entitiesByTypeAndId = model.getEntitesByTypeAndId()

        entitiesByTypeAndId.forEach { (type, entities) ->
            val entitiesToKeep =
                when (type) {
                    NetexTypes.LINE -> {
                        // Filter out Lines that are marked for removal
                        entities.filter { (lineId) ->
                            val shouldRemove = lineId in lineFilteringRepository.lineIdsToRemove
                            if (shouldRemove) {
                                logger.info(
                                    "Removing Line entity",
                                    kv("lineId", lineId),
                                )
                            }
                            !shouldRemove
                        }
                    }
                    else -> {
                        // Keep all other entity types unchanged
                        entities
                    }
                }

            if (entitiesToKeep.isNotEmpty()) {
                activeEntitiesMap[type] = entitiesToKeep.toMutableMap()
            }
        }

        logger.info(
            "Line filtering complete",
            kv("linesRemoved", lineFilteringRepository.lineIdsToRemove.size),
        )

        return EntitySelection(activeEntitiesMap, model)
    }
}
