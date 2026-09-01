package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.lib.config.FilterConfig
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.sax.handlers.BookWhenFilterHandler
import org.entur.ror.ubelluris.sax.handlers.PublicationTimestampHandler
import org.entur.ror.ubelluris.sax.handlers.ServiceJourneyInterchangeDeduplicationHandler
import org.entur.ror.ubelluris.sax.handlers.TimetabledPassingTimeIdHandler
import org.entur.ror.ubelluris.sax.handlers.VersionRefNormalizerHandler
import org.entur.ror.ubelluris.sax.plugins.LineOperatorEnricher
import org.entur.ror.ubelluris.sax.plugins.LinePublicCodeFilterPlugin
import org.entur.ror.ubelluris.sax.plugins.ServiceJourneyInterchangeCollectorPlugin
import org.entur.ror.ubelluris.sax.plugins.TransportModeToLocalScheduledStopPointMapper
import org.entur.ror.ubelluris.sax.plugins.VersionRefNormalizerPlugin
import org.entur.ror.ubelluris.sax.selectors.entities.CascadingLineRemovalSelector

class TimetableFilterConfig(
    private val cliConfig: CliConfig,
) : FilterProfileConfiguration {
    val transportModeToLocalScheduledStopPointMapper = TransportModeToLocalScheduledStopPointMapper(cliConfig.transportModes)
    val interchangeCollectorPlugin = ServiceJourneyInterchangeCollectorPlugin()
    val lineOperatorEnricher = LineOperatorEnricher()
    val versionRefNormalizerPlugin = VersionRefNormalizerPlugin()

    private val linePublicCodeRegexPatterns =
        listOf(
            Regex("\\s*(NO\\s*\\d+(?:\\s*[,/]\\s*\\d+)*\\s*[,/]?)\\s*"),
        )

    val linePublicCodeFilterPlugin = LinePublicCodeFilterPlugin(linePublicCodeRegexPatterns)

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
        val serviceJourneyInterchangeChildElements =
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

        val versionRefNormalizerHandler = VersionRefNormalizerHandler(versionRefNormalizerPlugin.registry)
        val timetabledPassingTimeIdHandler = TimetabledPassingTimeIdHandler()

        val handlerMap =
            mutableMapOf(
                "/PublicationDelivery/PublicationTimestamp" to PublicationTimestampHandler(),
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/vehicleJourneys" +
                    "/ServiceJourney/FlexibleServiceProperties/BookWhen" to BookWhenFilterHandler(),
                // remember to also update plugins supported element types
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/vehicleJourneys" +
                    "/ServiceJourney/trainNumbers/TrainNumberRef" to versionRefNormalizerHandler,
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ResourceFrame/vehicles" +
                    "/Vehicle/VehicleTypeRef" to versionRefNormalizerHandler,
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/vehicleJourneys" +
                    "/ServiceJourney/passingTimes/TimetabledPassingTime" to timetabledPassingTimeIdHandler,
            )

        handlerMap[baseInterchangePath] = interchangeDeduplicationHandler
        // need to handle all child elements of ServiceJourneyInterchange to not leave dangling children
        serviceJourneyInterchangeChildElements.forEach { childElement ->
            handlerMap["$baseInterchangePath/$childElement"] = interchangeDeduplicationHandler
        }

        return FilterConfigBuilder()
            .withPlugins(
                listOf(
                    versionRefNormalizerPlugin,
                    transportModeToLocalScheduledStopPointMapper,
                    interchangeCollectorPlugin,
                    lineOperatorEnricher,
                    linePublicCodeFilterPlugin,
                ),
            ).withEntitySelectors(
                listOf(
                    CascadingLineRemovalSelector(linePublicCodeFilterPlugin.repository),
                ),
            ).withSkipElements(
                listOf(
                    "/PublicationDelivery/dataObjects/SiteFrame/topographicPlaces/TopographicPlace/CountryRef",
                ),
            ).withCustomElementHandlers(handlerMap)
            .withRemovePrivateData(false)
            .withPreserveComments(false)
            .withUseSelfClosingTagsWhereApplicable(true)
            .withPruneReferences(true)
            .withUnreferencedEntitiesToPrune(
                setOf(
                    // cleans up after line removal
                    "DestinationDisplay",
                    // cleans up authorities that do not provide contact details
                    "Authority",
                    "Operator",
                    "Network",
                ),
            ).build()
    }
}
