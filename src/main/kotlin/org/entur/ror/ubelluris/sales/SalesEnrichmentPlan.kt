package org.entur.ror.ubelluris.sales

import org.entur.ror.ubelluris.model.SalesData
import org.entur.ror.ubelluris.sax.plugins.SalesMatchDataCollector
import java.time.LocalDate

/**
 * Decides how the timetable data of one provider is enriched with the sales data:
 * - a matched rail ServiceJourney gets the sales KeyValues, and is converted: its dayTypes are replaced by
 *   one DatedServiceJourney per operating date,
 * - each operating date of a converted ServiceJourney becomes an OperatingDay.
 *
 * The plan is computed once, on first use. Use it only after [salesMatchDataCollector] has collected all files,
 * i.e. from entity selectors or export handlers.
 */
class SalesEnrichmentPlan(
    private val salesData: SalesData,
    private val salesMatchDataCollector: SalesMatchDataCollector,
    private val salesAuthorityNumberPriority: List<String> = emptyList(),
) {
    companion object {
        // The same version as the other elements in the Samtrafiken timetable data
        const val OPERATING_DAY_VERSION = "any"
    }

    data class OperatingDayReference(
        val operatingDayId: String,
        val date: LocalDate,
    )

    data class DatedServiceJourneyReference(
        val datedServiceJourneyId: String,
        val operatingDayId: String,
    )

    private class Plan(
        val matchesByServiceJourneyId: Map<String, SalesTimetableMatcher.ServiceJourneyMatch>,
        val operatingDatesByConvertedServiceJourneyId: Map<String, List<LocalDate>>,
    )

    private val plan: Plan by lazy {
        val calendar = salesMatchDataCollector.calendar()
        val railServiceJourneys = salesMatchDataCollector.railServiceJourneys()
        val matcher = SalesTimetableMatcher(salesData, calendar, salesAuthorityNumberPriority)
        val timetableOperatingDatesResolver = OperatingDatesResolver(calendar)

        val matchesByServiceJourneyId = railServiceJourneys.associate { it.serviceJourneyId to matcher.match(it) }
        // A match needs operating dates, so every converted ServiceJourney has at least one date
        val operatingDatesByConvertedServiceJourneyId =
            railServiceJourneys
                .filter { matchesByServiceJourneyId[it.serviceJourneyId]?.outcome == SalesTimetableMatcher.MatchOutcome.MATCHED }
                .associate { it.serviceJourneyId to timetableOperatingDatesResolver.resolve(it.dayTypeRefs).sorted() }
        Plan(matchesByServiceJourneyId, operatingDatesByConvertedServiceJourneyId)
    }

    /** Match counts of all rail ServiceJourneys the collector found. */
    val matchSummary: SalesTimetableMatcher.MatchSummary
        get() = SalesTimetableMatcher.MatchSummary.of(plan.matchesByServiceJourneyId.values)

    val convertedServiceJourneyIds: Set<String>
        get() = plan.operatingDatesByConvertedServiceJourneyId.keys

    /** The OperatingDays that the DatedServiceJourneys refer to, sorted by date. */
    val operatingDays: List<OperatingDayReference> by lazy {
        plan.operatingDatesByConvertedServiceJourneyId
            .flatMap { (serviceJourneyId, dates) -> dates.map { operatingDayReference(codespaceOf(serviceJourneyId), it) } }
            .distinct()
            .sortedWith(compareBy({ it.date }, { it.operatingDayId }))
    }

    fun salesValues(serviceJourneyId: String): SalesTimetableMatcher.SalesValues? =
        plan.matchesByServiceJourneyId[serviceJourneyId]?.salesValues

    fun isConverted(serviceJourneyId: String): Boolean = serviceJourneyId in plan.operatingDatesByConvertedServiceJourneyId

    /** One DatedServiceJourney per operating date of a converted ServiceJourney. Empty when it is not converted. */
    fun datedServiceJourneys(serviceJourneyId: String): List<DatedServiceJourneyReference> {
        val codespace = codespaceOf(serviceJourneyId)
        val localId = serviceJourneyId.substringAfterLast(':')
        return plan.operatingDatesByConvertedServiceJourneyId[serviceJourneyId].orEmpty().map { date ->
            DatedServiceJourneyReference(
                datedServiceJourneyId = "$codespace:DatedServiceJourney:${localId}_$date",
                operatingDayId = operatingDayReference(codespace, date).operatingDayId,
            )
        }
    }

    private fun operatingDayReference(
        codespace: String,
        date: LocalDate,
    ) = OperatingDayReference("$codespace:OperatingDay:$date", date)

    // NeTEx ids are [codespace]:[ObjectType]:[localIdentifier], e.g. SE:100:ServiceJourney:740100030884000001 -> SE:100
    private fun codespaceOf(serviceJourneyId: String): String = serviceJourneyId.substringBeforeLast(':').substringBeforeLast(':')
}
