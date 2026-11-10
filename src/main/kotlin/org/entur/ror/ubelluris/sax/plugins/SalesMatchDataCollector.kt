package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.model.DayTypeDateAssignment
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.SharedCalendarData
import org.entur.ror.ubelluris.sales.TimetableRailServiceJourney
import org.entur.ror.ubelluris.utils.parseNetexDaysOfWeek
import org.xml.sax.Attributes
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Collects the timetable data needed to match ServiceJourneys with the sales data:
 * - from _shared_data: DayTypes, OperatingPeriods and DayTypeAssignments
 * - from line files: the Line TransportMode via JourneyPattern -> Route -> Line, TrainNumbers, and the ServiceJourneys
 */
class SalesMatchDataCollector : AbstractNetexPlugin() {
    companion object {
        private const val RAIL_TRANSPORT_MODE = "rail"
        private const val DAY_TYPE = "DayType"
        private const val OPERATING_PERIOD = "OperatingPeriod"
        private const val DAY_TYPE_ASSIGNMENT = "DayTypeAssignment"
        private const val TRAIN_NUMBER = NetexTypes.TRAIN_NUMBER

        private const val KEY_LIST = "keyList"

        /** Children of a ServiceJourney that come before keyList in the NeTEx schema, in schema order. */
        val ELEMENTS_BEFORE_KEY_LIST = listOf("validityConditions", "ValidBetween", "alternativeTexts")
        private val TEXT_ELEMENTS =
            setOf(
                "DaysOfWeek",
                "FromDate",
                "ToDate",
                "Date",
                "isAvailable",
                NetexTypes.TRANSPORT_MODE,
                "ForAdvertisement",
                NetexTypes.KEY,
            )
    }

    private class CollectedServiceJourney(
        val id: String,
    ) {
        var journeyPatternRef: String? = null
        var transportMode: String? = null
        val dayTypeRefs = mutableListOf<String>()
        val trainNumberRefs = mutableListOf<String>()
        var hasKeyList = false
        val keyListKeys = mutableSetOf<String>()
        val elementsBeforeKeyList = mutableSetOf<String>()
    }

    private class CollectedDayTypeAssignment {
        var dayTypeRef: String? = null
        var operatingPeriodRef: String? = null
        var date: LocalDate? = null
        var isAvailable: Boolean = true
    }

    private val daysOfWeekByDayTypeId = mutableMapOf<String, List<DayOfWeek>>()
    private val fromDateByOperatingPeriodId = mutableMapOf<String, LocalDateTime>()
    private val toDateByOperatingPeriodId = mutableMapOf<String, LocalDateTime>()
    private val operatingPeriodIdByDayTypeId = mutableMapOf<String, String>()
    private val dateAssignmentsByDayTypeId = mutableMapOf<String, MutableList<DayTypeDateAssignment>>()
    private var currentDayTypeAssignment: CollectedDayTypeAssignment? = null

    private val routeIdByJourneyPatternId = mutableMapOf<String, String>()
    private val lineIdByRouteId = mutableMapOf<String, String>()
    private val transportModeByLineId = mutableMapOf<String, String>()
    private val trainNrByTrainNumberId = mutableMapOf<String, String>()
    private val serviceJourneys = mutableListOf<CollectedServiceJourney>()
    private val keyListKeysByServiceJourneyId = mutableMapOf<String, Set<String>>()
    private val lastElementBeforeKeyListByServiceJourneyId = mutableMapOf<String, String>()
    private var currentServiceJourney: CollectedServiceJourney? = null

    private val elementText = StringBuilder()

    override fun getName(): String = javaClass.name

    override fun getDescription(): String =
        "Collects timetable calendar and ServiceJourney data to match ServiceJourneys with the sales data"

    override fun getSupportedElementTypes() =
        setOf(
            DAY_TYPE,
            "$DAY_TYPE/DaysOfWeek",
            "$OPERATING_PERIOD/FromDate",
            "$OPERATING_PERIOD/ToDate",
            DAY_TYPE_ASSIGNMENT,
            "$DAY_TYPE_ASSIGNMENT/OperatingPeriodRef",
            "$DAY_TYPE_ASSIGNMENT/DayTypeRef",
            "$DAY_TYPE_ASSIGNMENT/Date",
            "$DAY_TYPE_ASSIGNMENT/isAvailable",
            "${NetexTypes.JOURNEY_PATTERN}/${NetexTypes.ROUTE_REF}",
            "${NetexTypes.ROUTE}/${NetexTypes.LINE_REF}",
            "${NetexTypes.LINE}/${NetexTypes.TRANSPORT_MODE}",
            "$TRAIN_NUMBER/ForAdvertisement",
            NetexTypes.SERVICE_JOURNEY,
            "${NetexTypes.SERVICE_JOURNEY}/${NetexTypes.TRANSPORT_MODE}",
            "${NetexTypes.SERVICE_JOURNEY}/DayTypeRef",
            "${NetexTypes.SERVICE_JOURNEY}/${NetexTypes.JOURNEY_PATTERN_REF}",
            "${NetexTypes.SERVICE_JOURNEY}/TrainNumberRef",
            "${NetexTypes.SERVICE_JOURNEY}/$KEY_LIST",
            "${NetexTypes.KEY_VALUE}/${NetexTypes.KEY}",
        ) + ELEMENTS_BEFORE_KEY_LIST.map { "${NetexTypes.SERVICE_JOURNEY}/$it" }

    override fun startElement(
        elementName: String,
        attributes: Attributes?,
        currentEntity: Entity?,
    ) {
        if (elementName in TEXT_ELEMENTS) {
            elementText.clear()
        }
        val entity = currentEntity ?: return
        val ref = attributes?.getValue("ref")
        val serviceJourney = currentServiceJourney?.takeIf { entity.type == NetexTypes.SERVICE_JOURNEY && entity.id == it.id }

        when (elementName) {
            NetexTypes.SERVICE_JOURNEY ->
                if (entity.type == NetexTypes.SERVICE_JOURNEY) currentServiceJourney = CollectedServiceJourney(entity.id)
            DAY_TYPE -> if (entity.type == DAY_TYPE) daysOfWeekByDayTypeId.putIfAbsent(entity.id, emptyList())
            DAY_TYPE_ASSIGNMENT -> if (entity.type == DAY_TYPE_ASSIGNMENT) currentDayTypeAssignment = CollectedDayTypeAssignment()
            "OperatingPeriodRef" -> if (entity.type == DAY_TYPE_ASSIGNMENT) currentDayTypeAssignment?.operatingPeriodRef = ref
            "DayTypeRef" ->
                when (entity.type) {
                    DAY_TYPE_ASSIGNMENT -> currentDayTypeAssignment?.dayTypeRef = ref
                    NetexTypes.SERVICE_JOURNEY -> ref?.let { serviceJourney?.dayTypeRefs?.add(it) }
                }
            NetexTypes.ROUTE_REF -> if (entity.type == NetexTypes.JOURNEY_PATTERN && ref != null) routeIdByJourneyPatternId[entity.id] = ref
            NetexTypes.LINE_REF -> if (entity.type == NetexTypes.ROUTE && ref != null) lineIdByRouteId[entity.id] = ref
            NetexTypes.JOURNEY_PATTERN_REF -> serviceJourney?.journeyPatternRef = ref
            "TrainNumberRef" -> ref?.let { serviceJourney?.trainNumberRefs?.add(it) }
            KEY_LIST -> serviceJourney?.hasKeyList = true
            in ELEMENTS_BEFORE_KEY_LIST -> serviceJourney?.elementsBeforeKeyList?.add(elementName)
        }
    }

    override fun characters(
        elementName: String,
        ch: CharArray?,
        start: Int,
        length: Int,
    ) {
        if (elementName in TEXT_ELEMENTS && ch != null) {
            elementText.appendRange(ch, start, start + length)
        }
    }

    override fun endElement(
        elementName: String,
        currentEntity: Entity?,
    ) {
        val entity = currentEntity ?: return
        val text = elementText.toString().trim()

        when (elementName) {
            "DaysOfWeek" -> if (entity.type == DAY_TYPE) daysOfWeekByDayTypeId[entity.id] = parseNetexDaysOfWeek(text)
            "FromDate" -> if (entity.type == OPERATING_PERIOD) fromDateByOperatingPeriodId[entity.id] = LocalDateTime.parse(text)
            "ToDate" -> if (entity.type == OPERATING_PERIOD) toDateByOperatingPeriodId[entity.id] = LocalDateTime.parse(text)
            "Date" -> if (entity.type == DAY_TYPE_ASSIGNMENT) currentDayTypeAssignment?.date = LocalDate.parse(text)
            "isAvailable" -> if (entity.type == DAY_TYPE_ASSIGNMENT) currentDayTypeAssignment?.isAvailable = text.toBooleanStrict()
            "ForAdvertisement" -> if (entity.type == TRAIN_NUMBER) trainNrByTrainNumberId[entity.id] = text
            // Ignore entities that can have keylists, e.g. ServiceFacilitySet
            NetexTypes.KEY ->
                if (entity.type == NetexTypes.SERVICE_JOURNEY) currentServiceJourney?.takeIf { it.id == entity.id }?.keyListKeys?.add(text)
            NetexTypes.TRANSPORT_MODE ->
                when (entity.type) {
                    NetexTypes.LINE -> transportModeByLineId[entity.id] = text
                    NetexTypes.SERVICE_JOURNEY -> currentServiceJourney?.takeIf { it.id == entity.id }?.transportMode = text
                }
            DAY_TYPE_ASSIGNMENT -> if (entity.type == DAY_TYPE_ASSIGNMENT) finishDayTypeAssignment()
            NetexTypes.SERVICE_JOURNEY ->
                currentServiceJourney?.takeIf { entity.type == NetexTypes.SERVICE_JOURNEY && it.id == entity.id }?.let {
                    serviceJourneys.add(it)
                    if (it.hasKeyList) keyListKeysByServiceJourneyId[it.id] = it.keyListKeys.toSet()
                    ELEMENTS_BEFORE_KEY_LIST
                        .lastOrNull { elementName -> elementName in it.elementsBeforeKeyList }
                        ?.let { elementName -> lastElementBeforeKeyListByServiceJourneyId[it.id] = elementName }
                    currentServiceJourney = null
                }
        }
    }

    private fun finishDayTypeAssignment() {
        val assignment = currentDayTypeAssignment ?: return
        currentDayTypeAssignment = null
        val dayTypeRef = assignment.dayTypeRef ?: return
        val operatingPeriodRef = assignment.operatingPeriodRef
        val date = assignment.date
        when {
            operatingPeriodRef != null -> operatingPeriodIdByDayTypeId[dayTypeRef] = operatingPeriodRef
            date != null ->
                dateAssignmentsByDayTypeId.getOrPut(dayTypeRef) { mutableListOf() }.add(DayTypeDateAssignment(date, assignment.isAvailable))
        }
    }

    /** The calendar of the provider. OperatingPeriods without both FromDate and ToDate are left out. */
    fun calendar(): SharedCalendarData =
        SharedCalendarData(
            dayTypes = daysOfWeekByDayTypeId.toMap(),
            operatingPeriods =
                fromDateByOperatingPeriodId
                    .mapNotNull { (operatingPeriodId, fromDate) ->
                        toDateByOperatingPeriodId[operatingPeriodId]?.let { toDate -> operatingPeriodId to Pair(fromDate, toDate) }
                    }.toMap(),
            dayTypeAssignments = operatingPeriodIdByDayTypeId.toMap(),
            dayTypeDateAssignments = dateAssignmentsByDayTypeId.mapValues { (_, dates) -> dates.toList() },
        )

    /**
     * The ServiceJourneys that are trains. A ServiceJourney TransportMode overrides the Line TransportMode,
     * e.g. replacement buses on rail lines. TrainNumberRefs that do not resolve to a TrainNumber are left out.
     */
    fun railServiceJourneys(): List<TimetableRailServiceJourney> =
        serviceJourneys
            .filter { resolveTransportMode(it) == RAIL_TRANSPORT_MODE }
            .map { serviceJourney ->
                TimetableRailServiceJourney(
                    serviceJourneyId = serviceJourney.id,
                    trainNrs = serviceJourney.trainNumberRefs.mapNotNull { trainNrByTrainNumberId[it] },
                    dayTypeRefs = serviceJourney.dayTypeRefs.toList(),
                )
            }

    /** True when the ServiceJourney has its own keyList. A keyList of a nested entity, e.g. ServiceFacilitySet, does not count. */
    fun hasKeyList(serviceJourneyId: String): Boolean = serviceJourneyId in keyListKeysByServiceJourneyId

    /** The Keys in the ServiceJourney's own keyList. */
    fun keyListKeys(serviceJourneyId: String): Set<String> = keyListKeysByServiceJourneyId[serviceJourneyId].orEmpty()

    /** The last child of the ServiceJourney, in schema order, that must come before keyList, or null when it has none. */
    fun lastElementBeforeKeyList(serviceJourneyId: String): String? = lastElementBeforeKeyListByServiceJourneyId[serviceJourneyId]

    override fun getCollectedData(): List<TimetableRailServiceJourney> = railServiceJourneys()

    private fun resolveTransportMode(serviceJourney: CollectedServiceJourney): String? =
        serviceJourney.transportMode
            ?: serviceJourney.journeyPatternRef
                ?.let { routeIdByJourneyPatternId[it] }
                ?.let { lineIdByRouteId[it] }
                ?.let { transportModeByLineId[it] }
}
