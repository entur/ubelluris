package org.entur.ror.ubelluris.sales

import java.time.LocalDate

/**
 * How the timetable data of one provider is enriched with the sales data:
 * - a matched rail ServiceJourney gets the sales KeyValues, and is converted: its dayTypes are replaced by
 *   one DatedServiceJourney per operating date,
 * - each operating date of a converted ServiceJourney becomes an OperatingDay.
 *
 * [SalesEnrichmentPlanner] creates it. [restrictedTo] reduces it to the ServiceJourneys that the export writes.
 */
data class SalesEnrichment(
    /** Match counts of all rail ServiceJourneys of the provider, also the ones that the export leaves out. */
    val matchSummary: SalesTimetableMatcher.MatchSummary,
    val convertedServiceJourneysById: Map<String, ConvertedServiceJourney>,
) {
    companion object {
        // The same version as the other elements in the Samtrafiken timetable data
        const val OPERATING_DAY_VERSION = "any"

        val NONE = SalesEnrichment(SalesTimetableMatcher.MatchSummary(), emptyMap())
    }

    /** A matched rail ServiceJourney. A match needs operating dates, so [operatingDates] is never empty. */
    data class ConvertedServiceJourney(
        val salesValues: SalesTimetableMatcher.SalesValues,
        val operatingDates: List<LocalDate>,
    )

    data class OperatingDayReference(
        val operatingDayId: String,
        val date: LocalDate,
    )

    data class DatedServiceJourneyReference(
        val datedServiceJourneyId: String,
        val operatingDayId: String,
    )

    val convertedServiceJourneyIds: Set<String>
        get() = convertedServiceJourneysById.keys

    /** The OperatingDays that the DatedServiceJourneys refer to, sorted by date. */
    val operatingDays: List<OperatingDayReference>
        get() =
            convertedServiceJourneysById
                .flatMap { (serviceJourneyId, convertedServiceJourney) ->
                    convertedServiceJourney.operatingDates.map { operatingDayReference(codespaceOf(serviceJourneyId), it) }
                }.distinct()
                .sortedWith(compareBy({ it.date }, { it.operatingDayId }))

    fun isConverted(serviceJourneyId: String): Boolean = serviceJourneyId in convertedServiceJourneysById

    fun salesValues(serviceJourneyId: String): SalesTimetableMatcher.SalesValues? =
        convertedServiceJourneysById[serviceJourneyId]?.salesValues

    /** One DatedServiceJourney per operating date of a converted ServiceJourney. Empty when it is not converted. */
    fun datedServiceJourneys(serviceJourneyId: String): List<DatedServiceJourneyReference> {
        val codespace = codespaceOf(serviceJourneyId)
        val localId = serviceJourneyId.substringAfterLast(':')
        return convertedServiceJourneysById[serviceJourneyId]?.operatingDates.orEmpty().map { date ->
            DatedServiceJourneyReference(
                datedServiceJourneyId = "$codespace:DatedServiceJourney:${localId}_$date",
                operatingDayId = operatingDayReference(codespace, date).operatingDayId,
            )
        }
    }

    /** Keeps only the converted ServiceJourneys in [serviceJourneyIds]. The [matchSummary] does not change. */
    fun restrictedTo(serviceJourneyIds: Set<String>): SalesEnrichment =
        copy(convertedServiceJourneysById = convertedServiceJourneysById.filterKeys { it in serviceJourneyIds })

    private fun operatingDayReference(
        codespace: String,
        date: LocalDate,
    ) = OperatingDayReference("$codespace:OperatingDay:$date", date)

    // NeTEx ids are [codespace]:[ObjectType]:[localIdentifier], e.g. SE:100:ServiceJourney:740100030884000001 -> SE:100
    private fun codespaceOf(serviceJourneyId: String): String = serviceJourneyId.substringBeforeLast(':').substringBeforeLast(':')
}
