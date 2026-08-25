package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.lib.config.FilterConfig
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.sax.handlers.PublicationTimestampHandler
import org.entur.ror.ubelluris.sax.handlers.ServiceJourneyInterchangeDeduplicationHandler
import org.entur.ror.ubelluris.sax.plugins.LineOperatorEnricher
import org.entur.ror.ubelluris.sax.plugins.ServiceJourneyInterchangeCollectorPlugin
import org.entur.ror.ubelluris.sax.plugins.TransportModeToLocalScheduledStopPointMapper

class TimetableFilterConfig(
    private val cliConfig: CliConfig,
) : FilterProfileConfiguration {
    val plugin = TransportModeToLocalScheduledStopPointMapper(cliConfig.transportModes)
    val interchangeCollectorPlugin = ServiceJourneyInterchangeCollectorPlugin()
    val lineOperatorEnricher = LineOperatorEnricher()

    override fun build(): FilterConfig {
        val baseInterchangePath =
            "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/journeyInterchanges/ServiceJourneyInterchange"

        val interchangeDeduplicationHandler = ServiceJourneyInterchangeDeduplicationHandler(interchangeCollectorPlugin)

        // List of all possible child elements of ServiceJourneyInterchange according to NeTEx schema
        // Source: NeTEx Part 1, Section 6.4 - Interchange (Connection) elements
        // Reference: https://www.netex-cen.eu/
        //
        // NOTE: If the NeTEx schema adds new child elements to ServiceJourneyInterchange,
        // they MUST be added to this list to ensure proper deduplication.
        // Missing elements will be written even for duplicate ServiceJourneyInterchange elements.
        // TODO: find a way to skip entire ServiceJourneyInterchange element without having to register all child elements explicitly.
        val childElements =
            listOf(
                // Core properties
                "Priority",
                "Guaranteed",
                "Advertised",
                "Planned",
                "StaySeated",
                // Timing properties
                "MaximumWaitTime",
                "StandardWaitTime",
                "MaximumAutomaticWaitTime",
                "StandardTransferTime",
                "MinimumTransferTime",
                "MaximumTransferTime",
                // Reference properties
                "FromPointRef",
                "ToPointRef",
                "FromJourneyRef",
                "ToJourneyRef",
                // Descriptive properties
                "Description",
                "PrivateCode",
            )

        // Build handler map: register the same handler instance for parent and all children
        val handlerMap =
            mutableMapOf(
                "/PublicationDelivery/PublicationTimestamp" to PublicationTimestampHandler(),
                baseInterchangePath to interchangeDeduplicationHandler,
            )
        childElements.forEach { childElement ->
            handlerMap["$baseInterchangePath/$childElement"] = interchangeDeduplicationHandler
        }

        return FilterConfigBuilder()
            .withSkipElements(
                listOf(
                    "/PublicationDelivery/dataObjects/SiteFrame/topographicPlaces/TopographicPlace/CountryRef",
                ),
            ).withCustomElementHandlers(handlerMap)
            .withPlugins(listOf(plugin, interchangeCollectorPlugin, lineOperatorEnricher))
            .withRemovePrivateData(false)
            .withPreserveComments(false)
            .withUseSelfClosingTagsWhereApplicable(true)
            .withPruneReferences(false)
            .build()
    }
}
