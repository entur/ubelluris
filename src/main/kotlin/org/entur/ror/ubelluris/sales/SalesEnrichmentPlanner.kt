package org.entur.ror.ubelluris.sales

import org.entur.ror.ubelluris.model.SalesData
import org.entur.ror.ubelluris.sax.plugins.SalesMatchRepository

/**
 * Matches the collected rail ServiceJourneys of one provider with the sales data, and plans the [SalesEnrichment].
 * Call it after the SalesMatchDataCollector has collected all files, and before the entity selection.
 */
class SalesEnrichmentPlanner(
    private val salesData: SalesData,
    private val salesAuthorityNumberPriority: List<String> = emptyList(),
) {
    fun plan(salesMatchRepository: SalesMatchRepository): SalesEnrichment {
        // Without sales data, e.g. for a provider without sales patches, nothing is matched and nothing is logged as unmatched
        if (salesData.trainNrServiceJourneysMap.isEmpty()) {
            return SalesEnrichment.NONE
        }
        val matcher = SalesTimetableMatcher(salesData, salesMatchRepository.calendar(), salesAuthorityNumberPriority)
        val railServiceJourneys = salesMatchRepository.railServiceJourneys()
        val matches = railServiceJourneys.associateWith { matcher.match(it) }

        val convertedServiceJourneysById =
            matches
                .mapNotNull { (serviceJourney, match) ->
                    match.salesValues?.let { salesValues ->
                        serviceJourney.serviceJourneyId to
                            SalesEnrichment.ConvertedServiceJourney(salesValues, matcher.timetableOperatingDates(serviceJourney).sorted())
                    }
                }.toMap()
        return SalesEnrichment(SalesTimetableMatcher.MatchSummary.of(matches.values), convertedServiceJourneysById)
    }
}
