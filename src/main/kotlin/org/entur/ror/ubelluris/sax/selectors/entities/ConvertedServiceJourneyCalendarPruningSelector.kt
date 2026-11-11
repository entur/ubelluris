package org.entur.ror.ubelluris.sax.selectors.entities

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.model.EntityModel
import org.entur.netex.tools.lib.selections.EntitySelection
import org.entur.netex.tools.lib.selectors.entities.EntitySelector
import org.entur.netex.tools.lib.selectors.entities.EntitySelectorContext
import org.entur.ror.ubelluris.sales.SalesEnrichmentPlan
import org.slf4j.LoggerFactory

/**
 * Removes the calendar data that only the ServiceJourneys converted to DatedServiceJourneys used:
 * - DayTypes that a converted ServiceJourney refers to, and that no other ServiceJourney or DatedServiceJourney refers to,
 * - the DayTypeAssignments of those DayTypes,
 * - OperatingPeriods that only those DayTypeAssignments refer to.
 */
class ConvertedServiceJourneyCalendarPruningSelector(
    private val salesEnrichmentPlan: SalesEnrichmentPlan,
) : EntitySelector {
    companion object {
        private const val DAY_TYPE_REF = "DayTypeRef"
        private const val OPERATING_PERIOD_REF = "OperatingPeriodRef"
        private const val DAY_TYPE_ASSIGNMENT = "DayTypeAssignment"

        // The entity types that refer to DayTypes from their dayTypes
        private val DAY_TYPE_USERS = listOf("ServiceJourney", "DatedServiceJourney")
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun selectEntities(context: EntitySelectorContext): EntitySelection {
        val model = context.entityModel
        val convertedServiceJourneyIds = salesEnrichmentPlan.convertedServiceJourneyIds

        val dayTypeIdsOfConvertedServiceJourneys = convertedServiceJourneyIds.flatMap { refsFrom(model, it, DAY_TYPE_REF) }.toSet()
        val dayTypeIdsStillUsed =
            DAY_TYPE_USERS
                .flatMap { model.getEntitiesOfType(it) }
                .filter { it.id !in convertedServiceJourneyIds }
                .flatMap { refsFrom(model, it.id, DAY_TYPE_REF) }
                .toSet()
        val dayTypeIdsToRemove = dayTypeIdsOfConvertedServiceJourneys - dayTypeIdsStillUsed

        val (dayTypeAssignmentsToRemove, dayTypeAssignmentsToKeep) =
            model.getEntitiesOfType(DAY_TYPE_ASSIGNMENT).partition { assignment ->
                refsFrom(model, assignment.id, DAY_TYPE_REF).any { it in dayTypeIdsToRemove }
            }
        val operatingPeriodIdsToRemove =
            dayTypeAssignmentsToRemove.flatMap { refsFrom(model, it.id, OPERATING_PERIOD_REF) }.toSet() -
                dayTypeAssignmentsToKeep.flatMap { refsFrom(model, it.id, OPERATING_PERIOD_REF) }.toSet()

        val entityIdsToRemove = dayTypeIdsToRemove + dayTypeAssignmentsToRemove.map { it.id } + operatingPeriodIdsToRemove
        logger.info(
            "Removing calendar data of converted service journeys: {} day types, {} day type assignments, {} operating periods",
            dayTypeIdsToRemove.size,
            dayTypeAssignmentsToRemove.size,
            operatingPeriodIdsToRemove.size,
        )

        val keptEntitiesByType = mutableMapOf<String, MutableMap<String, Entity>>()
        model.getEntitesByTypeAndId().forEach { (type, entitiesById) ->
            val keptEntities = entitiesById.filterKeys { it !in entityIdsToRemove }
            if (keptEntities.isNotEmpty()) {
                keptEntitiesByType[type] = keptEntities.toMutableMap()
            }
        }
        return EntitySelection(keptEntitiesByType, model)
    }

    private fun refsFrom(
        model: EntityModel,
        entityId: String,
        refType: String,
    ): List<String> = model.getRefsOfTypeFrom(entityId, refType).map { it.ref }
}
