package org.entur.ror.ubelluris.sax.selectors.entities

import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.selections.EntitySelection
import org.entur.netex.tools.lib.selectors.entities.EntitySelector
import org.entur.netex.tools.lib.selectors.entities.EntitySelectorContext
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.ServiceJourneyInterchangeCollectorPlugin
import org.slf4j.LoggerFactory

/**
 * Removes duplicate ServiceJourneyInterchange elements based on their content.
 * - If duplicates have identical content: keeps only the first occurrence
 * - If duplicates have different content: removes all occurrences
 */
class ServiceJourneyInterchangeDeduplicationSelector(
    private val collectorPlugin: ServiceJourneyInterchangeCollectorPlugin,
) : EntitySelector {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun selectEntities(context: EntitySelectorContext): EntitySelection {
        val model = context.entityModel
        val activeEntitiesMap = mutableMapOf<String, MutableMap<String, Entity>>()
        val entitiesByTypeAndId = model.getEntitesByTypeAndId()

        val identicalDuplicates = collectorPlugin.getIdenticalDuplicates()
        val conflictingDuplicates = collectorPlugin.getConflictingDuplicates()

        var identicalDuplicatesRemoved = 0
        var conflictingDuplicatesRemoved = 0

        entitiesByTypeAndId.forEach { (type, entities) ->
            val entitiesToKeep =
                if (type == NetexTypes.SERVICE_JOURNEY_INTERCHANGE) {
                    // Track seen entity IDs to keep only first occurrence of identical duplicates
                    val seenIds = mutableSetOf<String>()

                    entities.filter { entity ->
                        val entityId = entity.value.id

                        when {
                            // Remove all occurrences of conflicting duplicates
                            entityId in conflictingDuplicates -> {
                                conflictingDuplicatesRemoved++
                                logger.debug(
                                    "Removing conflicting duplicate ServiceJourneyInterchange: {}",
                                    kv("interchange_id", entityId),
                                )
                                false
                            }
                            // For identical duplicates, keep only the first occurrence
                            entityId in identicalDuplicates -> {
                                if (entityId !in seenIds) {
                                    seenIds.add(entityId)
                                    true
                                } else {
                                    identicalDuplicatesRemoved++
                                    logger.debug(
                                        "Removing identical duplicate ServiceJourneyInterchange: {}",
                                        kv("interchange_id", entityId),
                                    )
                                    false
                                }
                            }
                            // Not a duplicate, keep it
                            else -> true
                        }
                    }
                } else {
                    // Keep all entities of other types unchanged
                    entities
                }

            activeEntitiesMap[type] = entitiesToKeep.toMutableMap()
        }

        if (identicalDuplicatesRemoved > 0 || conflictingDuplicatesRemoved > 0) {
            logger.info(
                "Removed ServiceJourneyInterchange duplicates - identical: {}, conflicting: {}, total: {}",
                kv("identical_duplicates_removed", identicalDuplicatesRemoved),
                kv("conflicting_duplicates_removed", conflictingDuplicatesRemoved),
                kv("total_removed", identicalDuplicatesRemoved + conflictingDuplicatesRemoved),
            )
        }

        return EntitySelection(activeEntitiesMap, model)
    }
}
