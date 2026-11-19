package org.entur.ror.ubelluris.sales

import org.entur.ror.ubelluris.model.SalesData
import org.entur.ror.ubelluris.model.SharedCalendarData
import org.slf4j.LoggerFactory
import java.time.LocalDate

/** A rail ServiceJourney of the timetable data, with the values needed to match it with the sales data. */
data class TimetableRailServiceJourney(
    val serviceJourneyId: String,
    val trainNrs: List<String>,
    val dayTypeRefs: List<String>,
)

/**
 * Matches the rail ServiceJourneys of one provider's timetable data with the sales ServiceJourneys.
 * A match needs one train number that is in the sales data, overlapping operating dates, and one
 * productCode/salesAuthorityNumber among the overlapping sales ServiceJourneys. Uses [salesAuthorityNumberPriority]
 * as a priority list when the overlapping sales ServiceJourneys have different values.
 * ServiceJourneys that cannot be matched are logged.
 */
class SalesTimetableMatcher(
    private val salesData: SalesData,
    timetableCalendar: SharedCalendarData,
    private val salesAuthorityNumberPriority: List<String> = emptyList(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val salesOperatingDatesResolver = OperatingDatesResolver(salesData.calendarData)
    private val timetableOperatingDatesResolver = OperatingDatesResolver(timetableCalendar)

    data class SalesValues(
        val productCode: String,
        val salesAuthorityNumber: String,
    )

    enum class MatchOutcome {
        MATCHED,
        WITHOUT_TRAIN_NR,
        MULTIPLE_TRAIN_NRS,
        TRAIN_NR_NOT_IN_SALES,
        WITHOUT_OPERATING_DATES,
        NO_OPERATING_DATE_OVERLAP,
        AMBIGUOUS_OPERATING_DATE_MATCH,
    }

    /** The match result of one rail ServiceJourney. [salesValues] is set only for [MatchOutcome.MATCHED]. */
    data class ServiceJourneyMatch(
        val serviceJourneyId: String,
        val outcome: MatchOutcome,
        val salesValues: SalesValues? = null,
    )

    /** Match counts of the rail ServiceJourneys of one provider. Each rail ServiceJourney is counted in exactly one outcome. */
    data class MatchSummary(
        val railServiceJourneys: Int = 0,
        val matched: Int = 0,
        val withoutTrainNr: Int = 0,
        val multipleTrainNrs: Int = 0,
        val trainNrNotInSales: Int = 0,
        val withoutOperatingDates: Int = 0,
        val noOperatingDateOverlap: Int = 0,
        val ambiguousOperatingDateMatch: Int = 0,
    ) {
        companion object {
            fun of(matches: Collection<ServiceJourneyMatch>): MatchSummary {
                val outcomeCounts = matches.groupingBy { it.outcome }.eachCount()
                return MatchSummary(
                    railServiceJourneys = matches.size,
                    matched = outcomeCounts[MatchOutcome.MATCHED] ?: 0,
                    withoutTrainNr = outcomeCounts[MatchOutcome.WITHOUT_TRAIN_NR] ?: 0,
                    multipleTrainNrs = outcomeCounts[MatchOutcome.MULTIPLE_TRAIN_NRS] ?: 0,
                    trainNrNotInSales = outcomeCounts[MatchOutcome.TRAIN_NR_NOT_IN_SALES] ?: 0,
                    withoutOperatingDates = outcomeCounts[MatchOutcome.WITHOUT_OPERATING_DATES] ?: 0,
                    noOperatingDateOverlap = outcomeCounts[MatchOutcome.NO_OPERATING_DATE_OVERLAP] ?: 0,
                    ambiguousOperatingDateMatch = outcomeCounts[MatchOutcome.AMBIGUOUS_OPERATING_DATE_MATCH] ?: 0,
                )
            }
        }
    }

    fun match(serviceJourney: TimetableRailServiceJourney): ServiceJourneyMatch {
        val serviceJourneyId = serviceJourney.serviceJourneyId

        fun unmatched(outcome: MatchOutcome) = ServiceJourneyMatch(serviceJourneyId, outcome)

        val trainNr =
            when (serviceJourney.trainNrs.size) {
                0 -> {
                    logger.warn("Timetable service journey {} is a train without trainNr", serviceJourneyId)
                    return unmatched(MatchOutcome.WITHOUT_TRAIN_NR)
                }
                1 -> serviceJourney.trainNrs.single()
                else -> {
                    logger.warn("Timetable service journey {} has more than one trainNr {}", serviceJourneyId, serviceJourney.trainNrs)
                    return unmatched(MatchOutcome.MULTIPLE_TRAIN_NRS)
                }
            }

        val salesServiceJourneys = salesData.trainNrServiceJourneysMap[trainNr]
        if (salesServiceJourneys == null) {
            logger.warn("Timetable service journey {} has trainNr {} that is not in the sales data", serviceJourneyId, trainNr)
            return unmatched(MatchOutcome.TRAIN_NR_NOT_IN_SALES)
        }

        val timetableOperatingDates = timetableOperatingDates(serviceJourney)
        if (timetableOperatingDates.isEmpty()) {
            logger.warn("Timetable service journey {} with trainNr {} has no operating dates", serviceJourneyId, trainNr)
            return unmatched(MatchOutcome.WITHOUT_OPERATING_DATES)
        }

        val overlappingSalesServiceJourneys =
            salesServiceJourneys.filter { salesServiceJourney ->
                salesOperatingDatesResolver.resolve(salesServiceJourney.dayTypeRefs).any { it in timetableOperatingDates }
            }
        if (overlappingSalesServiceJourneys.isEmpty()) {
            logger.warn(
                "Timetable service journey {} with trainNr {} operates {}, which does not overlap any of the {} sales service journeys",
                serviceJourneyId,
                trainNr,
                describeDates(timetableOperatingDates),
                salesServiceJourneys.size,
            )
            return unmatched(MatchOutcome.NO_OPERATING_DATE_OVERLAP)
        }

        val distinctSalesValues =
            overlappingSalesServiceJourneys.map { SalesValues(it.productCode, it.salesAuthorityNumber) }.distinct()
        val prioritizedSalesValues = prioritizedSalesValues(distinctSalesValues)
        if (prioritizedSalesValues.size > 1) {
            logger.warn(
                "Timetable service journey {} with trainNr {} overlaps {} sales service journeys with different " +
                    "productCode/salesAuthorityNumber {} and no salesAuthorityNumber priority decides between them: {}",
                serviceJourneyId,
                trainNr,
                overlappingSalesServiceJourneys.size,
                distinctSalesValues,
                overlappingSalesServiceJourneys.map { it.serviceJourneyId },
            )
            return unmatched(MatchOutcome.AMBIGUOUS_OPERATING_DATE_MATCH)
        }

        return ServiceJourneyMatch(serviceJourneyId, MatchOutcome.MATCHED, prioritizedSalesValues.single())
    }

    /** The operating dates of a timetable ServiceJourney. The dates of each DayType are cached. */
    fun timetableOperatingDates(serviceJourney: TimetableRailServiceJourney): Set<LocalDate> =
        timetableOperatingDatesResolver.resolve(serviceJourney.dayTypeRefs)

    // Keeps the values with the highest-priority salesAuthorityNumber. Returns all values when none has a priority.
    private fun prioritizedSalesValues(distinctSalesValues: List<SalesValues>): List<SalesValues> {
        if (distinctSalesValues.size <= 1) {
            return distinctSalesValues
        }
        val highestPrioritySalesAuthorityNumber =
            salesAuthorityNumberPriority.firstOrNull { prioritized -> distinctSalesValues.any { it.salesAuthorityNumber == prioritized } }
                ?: return distinctSalesValues
        return distinctSalesValues.filter { it.salesAuthorityNumber == highestPrioritySalesAuthorityNumber }
    }

    private fun describeDates(dates: Set<LocalDate>): String = "${dates.size} dates from ${dates.min()} to ${dates.max()}"
}
