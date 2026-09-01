package org.entur.ror.ubelluris.sax.selectors.entities

import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.model.EntityModel
import org.entur.netex.tools.lib.selections.EntitySelection
import org.entur.netex.tools.lib.selectors.entities.EntitySelector
import org.entur.netex.tools.lib.selectors.entities.EntitySelectorContext
import org.entur.ror.ubelluris.sax.plugins.LineFilteringRepository
import org.slf4j.LoggerFactory

/**
 * Selects entities for removal based on a cascading effect starting from Lines marked for removal.
 */
class CascadingLineRemovalSelector(
    private val lineFilteringRepository: LineFilteringRepository,
) : EntitySelector {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun selectEntities(context: EntitySelectorContext): EntitySelection {
        logger.info(
            "CascadingLineRemovalSelector invoked",
            kv("lineIdsToRemove", lineFilteringRepository.lineIdsToRemove),
        )

        val model = context.entityModel
        val entitiesToRemove = mutableSetOf<String>()

        // Start with lines to remove
        entitiesToRemove.addAll(lineFilteringRepository.lineIdsToRemove)

        // Cascade: Remove Routes that reference removed Lines
        removeEntitiesReferencingRemovedEntities(
            model,
            entitiesToRemove,
            entityType = "Route",
            refType = "LineRef",
        )

        // Cascade: Remove ServiceJourneys that reference removed Routes directly (without a JourneyPattern)
        removeEntitiesReferencingRemovedEntities(
            model,
            entitiesToRemove,
            entityType = "ServiceJourney",
            refType = "RouteRef",
        )

        // Cascade: Remove JourneyPatterns that reference removed Routes
        removeEntitiesReferencingRemovedEntities(
            model,
            entitiesToRemove,
            entityType = "JourneyPattern",
            refType = "RouteRef",
        )

        // Cascade: Remove ServiceJourneys that reference Lines directly (without a JourneyPattern)
        removeEntitiesReferencingRemovedEntities(
            model,
            entitiesToRemove,
            entityType = "ServiceJourney",
            refType = "LineRef",
        )

        // Cascade: Remove ServiceJourneys that reference removed JourneyPatterns
        removeEntitiesReferencingRemovedEntities(
            model,
            entitiesToRemove,
            entityType = "ServiceJourney",
            refType = "JourneyPatternRef",
        )

        // Build selection excluding removed entities
        val activeEntitiesMap = mutableMapOf<String, MutableMap<String, Entity>>()
        model.getEntitesByTypeAndId().forEach { (type, entities) ->
            val keptEntities = entities.filterKeys { id -> id !in entitiesToRemove }
            if (keptEntities.isNotEmpty()) {
                activeEntitiesMap[type] = keptEntities.toMutableMap()
            }
        }

        return EntitySelection(activeEntitiesMap, model)
    }

    private fun removeEntitiesReferencingRemovedEntities(
        model: EntityModel,
        entitiesToRemove: MutableSet<String>,
        entityType: String,
        refType: String,
    ) {
        val entitiesToAdd =
            model
                .getEntitiesOfType(entityType)
                .filter { entity ->
                    val refs = model.getRefsOfTypeFrom(entity.id, refType)
                    refs.any { ref -> ref.ref in entitiesToRemove }
                }
        entitiesToRemove.addAll(entitiesToAdd.map { it.id })
    }
}
