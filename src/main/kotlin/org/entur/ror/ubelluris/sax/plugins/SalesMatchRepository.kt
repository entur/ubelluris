package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.selections.EntitySelection
import org.entur.netex.tools.lib.selections.RefSelection
import org.entur.ror.ubelluris.filter.SelectionDependent
import org.entur.ror.ubelluris.model.DayTypeDateAssignment
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.SharedCalendarData
import org.entur.ror.ubelluris.sales.SalesEnrichment
import org.entur.ror.ubelluris.sales.TimetableRailServiceJourney
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/** A ServiceJourney as [SalesMatchDataCollector] collected it. */
data class CollectedServiceJourney(
    val id: String,
    val journeyPatternRef: String?,
    val transportMode: String?,
    val dayTypeRefs: List<String>,
    val trainNumberRefs: List<String>,
    /** The Keys in the ServiceJourney's own keyList. Null when it has no keyList of its own. */
    val keyListKeys: Set<String>?,
    /** The last child, in schema order, that must come before keyList. Null when it has none. */
    val lastElementBeforeKeyList: String?,
)

/**
 * The only connection between the sales steps of the timetable filter. It has two sections:
 * - collected data: [SalesMatchDataCollector] writes it while it reads the files.
 *   Read it only after the collector has collected all files.
 * - [salesEnrichment]: TimetableFilterConfig.run writes it after the collection and before the entity selection.
 *   [updateBasedOnSelection] reduces it to the ServiceJourneys that the export writes.
 *
 * The repository throws on invalid data: duplicate ServiceJourney ids, OperatingPeriods without both FromDate and ToDate,
 * ServiceJourneys whose Line does not resolve, and TrainNumberRefs that do not resolve.
 */
class SalesMatchRepository : SelectionDependent {
    companion object {
        private const val RAIL_TRANSPORT_MODE = "rail"
    }

    private var plannedSalesEnrichment: SalesEnrichment? = null

    /** Entity selectors read all converted ServiceJourneys. Export handlers read only the exported ones. */
    var salesEnrichment: SalesEnrichment
        get() = checkNotNull(plannedSalesEnrichment) { "The sales enrichment is not planned before the collection is done" }
        set(salesEnrichment) {
            plannedSalesEnrichment = salesEnrichment
        }

    private val daysOfWeekByDayTypeId = mutableMapOf<String, List<DayOfWeek>>()
    private val fromDateByOperatingPeriodId = mutableMapOf<String, LocalDateTime>()
    private val toDateByOperatingPeriodId = mutableMapOf<String, LocalDateTime>()
    private val operatingPeriodIdByDayTypeId = mutableMapOf<String, String>()
    private val dateAssignmentsByDayTypeId = mutableMapOf<String, MutableList<DayTypeDateAssignment>>()

    private val routeIdByJourneyPatternId = mutableMapOf<String, String>()
    private val lineIdByRouteId = mutableMapOf<String, String>()
    private val transportModeByLineId = mutableMapOf<String, String>()
    private val trainNrByTrainNumberId = mutableMapOf<String, String>()
    private val serviceJourneysById = linkedMapOf<String, CollectedServiceJourney>()

    fun addDayType(dayTypeId: String) {
        daysOfWeekByDayTypeId.putIfAbsent(dayTypeId, emptyList())
    }

    fun addDaysOfWeek(
        dayTypeId: String,
        daysOfWeek: List<DayOfWeek>,
    ) {
        daysOfWeekByDayTypeId[dayTypeId] = daysOfWeek
    }

    fun addOperatingPeriodFromDate(
        operatingPeriodId: String,
        fromDate: LocalDateTime,
    ) {
        fromDateByOperatingPeriodId[operatingPeriodId] = fromDate
    }

    fun addOperatingPeriodToDate(
        operatingPeriodId: String,
        toDate: LocalDateTime,
    ) {
        toDateByOperatingPeriodId[operatingPeriodId] = toDate
    }

    fun addOperatingPeriodAssignment(
        dayTypeId: String,
        operatingPeriodId: String,
    ) {
        operatingPeriodIdByDayTypeId[dayTypeId] = operatingPeriodId
    }

    fun addDateAssignment(
        dayTypeId: String,
        date: LocalDate,
        isAvailable: Boolean,
    ) {
        dateAssignmentsByDayTypeId.getOrPut(dayTypeId) { mutableListOf() }.add(DayTypeDateAssignment(date, isAvailable))
    }

    fun addRouteRef(
        journeyPatternId: String,
        routeId: String,
    ) {
        routeIdByJourneyPatternId[journeyPatternId] = routeId
    }

    fun addLineRef(
        routeId: String,
        lineId: String,
    ) {
        lineIdByRouteId[routeId] = lineId
    }

    fun addLineTransportMode(
        lineId: String,
        transportMode: String,
    ) {
        transportModeByLineId[lineId] = transportMode
    }

    fun addTrainNr(
        trainNumberId: String,
        trainNr: String,
    ) {
        trainNrByTrainNumberId[trainNumberId] = trainNr
    }

    fun addServiceJourney(serviceJourney: CollectedServiceJourney) {
        check(serviceJourney.id !in serviceJourneysById) { "Duplicate ServiceJourney id: ${serviceJourney.id}" }
        serviceJourneysById[serviceJourney.id] = serviceJourney
    }

    /** The calendar of the provider. Fails when an OperatingPeriod does not have both FromDate and ToDate. */
    fun calendar(): SharedCalendarData {
        val operatingPeriodIdsWithFromDate = fromDateByOperatingPeriodId.keys
        val operatingPeriodIdsWithToDate = toDateByOperatingPeriodId.keys
        val incompleteOperatingPeriodIds =
            (operatingPeriodIdsWithFromDate union operatingPeriodIdsWithToDate) -
                (operatingPeriodIdsWithFromDate intersect operatingPeriodIdsWithToDate)
        check(incompleteOperatingPeriodIds.isEmpty()) {
            "OperatingPeriods without both FromDate and ToDate: ${incompleteOperatingPeriodIds.sorted()}"
        }
        return SharedCalendarData(
            dayTypes = daysOfWeekByDayTypeId.toMap(),
            operatingPeriods =
                fromDateByOperatingPeriodId.mapValues { (operatingPeriodId, fromDate) ->
                    Pair(fromDate, toDateByOperatingPeriodId.getValue(operatingPeriodId))
                },
            dayTypeAssignments = operatingPeriodIdByDayTypeId.toMap(),
            dayTypeDateAssignments = dateAssignmentsByDayTypeId.mapValues { (_, dates) -> dates.toList() },
        )
    }

    /**
     * The ServiceJourneys that are trains. A ServiceJourney TransportMode overrides the Line TransportMode,
     * e.g. replacement buses on rail lines. Fails when the Line or a TrainNumberRef of any ServiceJourney does not resolve.
     */
    fun railServiceJourneys(): List<TimetableRailServiceJourney> {
        serviceJourneysById.values.forEach(::requireResolvedReferences)
        return serviceJourneysById.values
            .filter { resolveTransportMode(it) == RAIL_TRANSPORT_MODE }
            .map { serviceJourney ->
                TimetableRailServiceJourney(
                    serviceJourneyId = serviceJourney.id,
                    trainNrs = serviceJourney.trainNumberRefs.map { trainNrByTrainNumberId.getValue(it) },
                    dayTypeRefs = serviceJourney.dayTypeRefs,
                )
            }
    }

    private fun requireResolvedReferences(serviceJourney: CollectedServiceJourney) {
        checkNotNull(lineId(serviceJourney.id)) {
            "Unresolved Line for ServiceJourney ${serviceJourney.id}: JourneyPatternRef -> RouteRef -> LineRef does not resolve"
        }
        val unresolvedTrainNumberRefs = serviceJourney.trainNumberRefs.filterNot { it in trainNrByTrainNumberId }
        check(unresolvedTrainNumberRefs.isEmpty()) {
            "Unresolved TrainNumberRefs in ServiceJourney ${serviceJourney.id}: $unresolvedTrainNumberRefs"
        }
    }

    /** True when the ServiceJourney has its own keyList. A keyList of a nested entity, e.g. ServiceFacilitySet, does not count. */
    fun hasKeyList(serviceJourneyId: String): Boolean = serviceJourneysById[serviceJourneyId]?.keyListKeys != null

    /** The Keys in the ServiceJourney's own keyList. */
    fun keyListKeys(serviceJourneyId: String): Set<String> = serviceJourneysById[serviceJourneyId]?.keyListKeys.orEmpty()

    /** The last child of the ServiceJourney, in schema order, that must come before keyList, or null when it has none. */
    fun lastElementBeforeKeyList(serviceJourneyId: String): String? = serviceJourneysById[serviceJourneyId]?.lastElementBeforeKeyList

    override fun updateBasedOnSelection(
        entitySelection: EntitySelection,
        refSelection: RefSelection,
    ) {
        val exportedServiceJourneyIds = entitySelection.selection[NetexTypes.SERVICE_JOURNEY]?.keys.orEmpty()
        salesEnrichment = salesEnrichment.restrictedTo(exportedServiceJourneyIds)
    }

    /** Follows ServiceJourney -> JourneyPatternRef -> RouteRef -> LineRef. Null when a reference does not resolve. */
    fun lineId(serviceJourneyId: String): String? =
        serviceJourneysById[serviceJourneyId]
            ?.journeyPatternRef
            ?.let { routeIdByJourneyPatternId[it] }
            ?.let { lineIdByRouteId[it] }

    private fun resolveTransportMode(serviceJourney: CollectedServiceJourney): String? =
        serviceJourney.transportMode ?: lineId(serviceJourney.id)?.let { transportModeByLineId[it] }
}
