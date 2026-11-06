package org.entur.ror.ubelluris.utils

import org.entur.ror.ubelluris.model.NetexTypes
import java.nio.file.Path

/**
 * The entities of one NeTEx line file that are needed to resolve the Line, TransportMode and train number of a ServiceJourney.
 */
class NetexLineFile private constructor(
    entities: List<CollectedNetexEntity>,
) {
    companion object {
        const val RAIL_TRANSPORT_MODE = "rail"

        private val entityCollector =
            NetexEntityCollector(
                setOf(
                    NetexTypes.LINE,
                    NetexTypes.ROUTE,
                    NetexTypes.JOURNEY_PATTERN,
                    NetexTypes.SERVICE_JOURNEY,
                    NetexTypes.TRAIN_NUMBER,
                ),
            )

        fun read(lineFilePath: Path): NetexLineFile = NetexLineFile(entityCollector.collect(lineFilePath))
    }

    private val entitiesByElementName = entities.groupBy { it.elementName }
    private val linesById = entitiesById(NetexTypes.LINE)
    private val routesById = entitiesById(NetexTypes.ROUTE)
    private val journeyPatternsById = entitiesById(NetexTypes.JOURNEY_PATTERN)
    private val trainNumbersById = entitiesById(NetexTypes.TRAIN_NUMBER)

    val serviceJourneys: List<CollectedNetexEntity> = entitiesByElementName[NetexTypes.SERVICE_JOURNEY].orEmpty()

    /** Follows ServiceJourney -> JourneyPatternRef -> RouteRef -> LineRef. */
    fun resolveLine(serviceJourney: CollectedNetexEntity): CollectedNetexEntity {
        val journeyPattern = journeyPatternsById.requireEntity(serviceJourney.requireValue(NetexTypes.JOURNEY_PATTERN_REF))
        val route = routesById.requireEntity(journeyPattern.requireValue(NetexTypes.ROUTE_REF))
        return linesById.requireEntity(route.requireValue(NetexTypes.LINE_REF))
    }

    /** A ServiceJourney TransportMode overrides the Line TransportMode, e.g. replacement buses on rail lines. */
    fun resolveTransportMode(
        serviceJourney: CollectedNetexEntity,
        line: CollectedNetexEntity,
    ): String? = serviceJourney.valueOrNull(NetexTypes.TRANSPORT_MODE) ?: line.valueOrNull(NetexTypes.TRANSPORT_MODE)

    /** Returns TrainNumber/ForAdvertisement of the single TrainNumberRef, or null when there is none or it does not resolve. */
    fun resolveTrainNr(serviceJourney: CollectedNetexEntity): String? {
        val trainNumbers =
            serviceJourney
                .values("trainNumbers/TrainNumberRef")
                .map { trainNumberRef -> trainNumbersById[trainNumberRef]?.requireValue("ForAdvertisement") }
        return when (trainNumbers.size) {
            0 -> null
            1 -> trainNumbers.first()
            else -> throw IllegalArgumentException(
                "Expected exactly one train number on service journey ${serviceJourney.id}, but found ${trainNumbers.size}",
            )
        }
    }

    private fun entitiesById(elementName: String) = entitiesByElementName[elementName].orEmpty().associateBy { it.id }

    private fun Map<String, CollectedNetexEntity>.requireEntity(ref: String): CollectedNetexEntity =
        this[ref] ?: error("Unresolved reference: $ref")
}
