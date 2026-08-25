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

    override fun build(): FilterConfig =
        FilterConfigBuilder()
            .withSkipElements(
                listOf(
                    "/PublicationDelivery/dataObjects/SiteFrame/topographicPlaces/TopographicPlace/CountryRef",
                ),
            ).withCustomElementHandlers(
                mapOf(
                    "/PublicationDelivery/PublicationTimestamp" to PublicationTimestampHandler(),
                    "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/journeyInterchanges/ServiceJourneyInterchange" to
                        ServiceJourneyInterchangeDeduplicationHandler(interchangeCollectorPlugin),
                ),
            ).withPlugins(listOf(plugin, interchangeCollectorPlugin, lineOperatorEnricher))
            .withRemovePrivateData(false)
            .withPreserveComments(false)
            .withUseSelfClosingTagsWhereApplicable(true)
            .withPruneReferences(false)
            .build()
}
