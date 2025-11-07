package filter

import org.entur.netex.tools.lib.config.FilterConfig
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.netex.tools.lib.config.TimePeriod
import java.time.LocalDate

class StandardImportFilterConfig : FilterProfileConfiguration {
    override fun build(): FilterConfig =
        FilterConfigBuilder()
            .withPeriod(
                TimePeriod(
                    start = LocalDate.now(),
                    end = LocalDate.now()
                )
            )
            .withSkipElements(
                listOf(
                    "/PublicationDelivery/dataObjects/SiteFrame/topographicPlaces",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/ShortName",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/alternativeNames",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/TopographicPlaceRef",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/quays/Quay/Name",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/quays/Quay/ShortName"
                )
            )
            .withRemovePrivateData(true)
            .withPreserveComments(false)
            .withUseSelfClosingTagsWhereApplicable(true)
            .withPruneReferences(true)
            .withReferencesToExcludeFromPruning(setOf("QuayRef"))
            .withUnreferencedEntitiesToPrune(
                setOf(
                    "JourneyPattern",
                    "Route",
                    "Network",
                    "Line",
                    "Operator",
                    "Notice",
                    "DestinationDisplay",
                    "ServiceLink",
                )
            )
            .build()
}