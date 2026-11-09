package org.entur.ror.ubelluris.sales

import org.entur.ror.ubelluris.model.SalesSharedData
import java.time.LocalDate

/**
 * Resolves the dates a ServiceJourney operates on from the DayTypes it refers to.
 *
 * The dates of one DayType are:
 * - the dates in its OperatingPeriod (FromDate inclusive, ToDate exclusive) that fall on one of its DaysOfWeek,
 * - plus its date assignments with isAvailable true,
 * - minus its date assignments with isAvailable false.
 * An unknown DayType has no dates.
 */
class OperatingDatesResolver(
    private val calendar: SalesSharedData,
) {
    private val datesByDayTypeId = mutableMapOf<String, Set<LocalDate>>()

    fun resolve(dayTypeRefs: List<String>): Set<LocalDate> = dayTypeRefs.flatMapTo(mutableSetOf()) { datesOfDayType(it) }

    private fun datesOfDayType(dayTypeId: String): Set<LocalDate> =
        datesByDayTypeId.getOrPut(dayTypeId) {
            val daysOfWeek = calendar.dayTypes[dayTypeId].orEmpty().toSet()
            val periodDates =
                calendar.dayTypeAssignments[dayTypeId]
                    ?.let { operatingPeriodId -> calendar.operatingPeriods[operatingPeriodId] }
                    ?.let { (fromDateTime, toDateTime) ->
                        generateSequence(fromDateTime.toLocalDate()) { it.plusDays(1) }
                            .takeWhile { it.atStartOfDay() < toDateTime }
                            .filter { it.dayOfWeek in daysOfWeek }
                            .toSet()
                    }.orEmpty()
            val (includedDates, excludedDates) = calendar.dayTypeDateAssignments[dayTypeId].orEmpty().partition { it.isAvailable }
            periodDates + includedDates.map { it.date } - excludedDates.map { it.date }.toSet()
        }
}
