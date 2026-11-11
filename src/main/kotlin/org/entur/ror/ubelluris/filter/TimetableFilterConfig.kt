package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.lib.config.FilterConfig
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.SalesData
import org.entur.ror.ubelluris.sales.SalesEnrichmentPlan
import org.entur.ror.ubelluris.sax.handlers.BookWhenFilterHandler
import org.entur.ror.ubelluris.sax.handlers.EmptyMembersFilterHandler
import org.entur.ror.ubelluris.sax.handlers.PublicationTimestampHandler
import org.entur.ror.ubelluris.sax.handlers.SalesOperatingDaysHandler
import org.entur.ror.ubelluris.sax.handlers.ServiceJourneyInterchangeDeduplicationHandler
import org.entur.ror.ubelluris.sax.handlers.ServiceJourneySalesHandler
import org.entur.ror.ubelluris.sax.handlers.StopPointInJourneyPatternBoardingAlightingHandler
import org.entur.ror.ubelluris.sax.handlers.TimetabledPassingTimeIdHandler
import org.entur.ror.ubelluris.sax.handlers.VersionRefNormalizerHandler
import org.entur.ror.ubelluris.sax.plugins.JourneyPatternStopPointCollectorPlugin
import org.entur.ror.ubelluris.sax.plugins.LineFilteringRepository
import org.entur.ror.ubelluris.sax.plugins.LineOperatorEnricher
import org.entur.ror.ubelluris.sax.plugins.LinePublicCodeFilterPlugin
import org.entur.ror.ubelluris.sax.plugins.LineTransportModeFilterPlugin
import org.entur.ror.ubelluris.sax.plugins.SalesMatchDataCollector
import org.entur.ror.ubelluris.sax.plugins.ServiceJourneyInterchangeCollectorPlugin
import org.entur.ror.ubelluris.sax.plugins.TransportModeToLocalScheduledStopPointMapper
import org.entur.ror.ubelluris.sax.plugins.VersionRefNormalizerPlugin
import org.entur.ror.ubelluris.sax.selectors.entities.CascadingLineRemovalSelector
import org.entur.ror.ubelluris.sax.selectors.entities.ConvertedServiceJourneyCalendarPruningSelector

class TimetableFilterConfig(
    cliConfig: CliConfig,
    salesData: SalesData,
) : FilterProfileConfiguration {
    private val features = cliConfig.timetableFilterFeatures

    val transportModeToLocalScheduledStopPointMapper = TransportModeToLocalScheduledStopPointMapper(cliConfig.transportModes)
    val interchangeCollectorPlugin = ServiceJourneyInterchangeCollectorPlugin()
    val journeyPatternStopPointCollectorPlugin = JourneyPatternStopPointCollectorPlugin()
    val lineOperatorEnricher = LineOperatorEnricher()
    val versionRefNormalizerPlugin = VersionRefNormalizerPlugin()
    val salesMatchDataCollector = SalesMatchDataCollector()
    val salesEnrichmentPlan = SalesEnrichmentPlan(salesData, salesMatchDataCollector, cliConfig.salesAuthorityNumberPriority)
    private val serviceJourneySalesHandler = ServiceJourneySalesHandler(salesEnrichmentPlan, salesMatchDataCollector)
    private val salesOperatingDaysHandler = SalesOperatingDaysHandler(salesEnrichmentPlan)

    private val linePublicCodeRegexPatterns =
        listOf(
            Regex("\\s*(NO\\s*\\d+(?:\\s*[,/]\\s*\\d+)*\\s*[,/]?)\\s*"),
        )

    val lineFilteringRepository = LineFilteringRepository()
    val linePublicCodeFilterPlugin =
        LinePublicCodeFilterPlugin(
            regexPatterns = linePublicCodeRegexPatterns,
            repository = lineFilteringRepository,
        )
    val lineTransportModeFilterPlugin =
        LineTransportModeFilterPlugin(
            transportModesToRemove = cliConfig.lineTransportModesToRemove.toSet(),
            repository = lineFilteringRepository,
        )

    override fun build(): FilterConfig {
        val handlerMap = mutableMapOf<String, XMLElementHandler>()

        if (features.publicationTimestamp) {
            handlerMap["/PublicationDelivery/PublicationTimestamp"] = PublicationTimestampHandler()
        }

        if (features.bookWhenFilter) {
            handlerMap[
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/vehicleJourneys" +
                    "/ServiceJourney/FlexibleServiceProperties/BookWhen",
            ] = BookWhenFilterHandler()
        }

        if (features.versionRefNormalizer) {
            val versionRefNormalizerHandler = VersionRefNormalizerHandler(versionRefNormalizerPlugin.registry)
            // remember to also update plugins supported element types
            handlerMap[
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/vehicleJourneys" +
                    "/ServiceJourney/trainNumbers/TrainNumberRef",
            ] = versionRefNormalizerHandler
            handlerMap[
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ResourceFrame/vehicles" +
                    "/Vehicle/VehicleTypeRef",
            ] = versionRefNormalizerHandler
        }

        if (features.timetabledPassingTimeId) {
            handlerMap[
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/vehicleJourneys" +
                    "/ServiceJourney/passingTimes/TimetabledPassingTime",
            ] = TimetabledPassingTimeIdHandler()
        }

        if (features.useSalesData) {
            ServiceJourneySalesHandler.handledPaths.forEach { path ->
                handlerMap[path] = serviceJourneySalesHandler
            }
            SalesOperatingDaysHandler.handledPaths.forEach { path ->
                handlerMap[path] = salesOperatingDaysHandler
            }
        }

        if (features.interchangeDeduplication) {
            registerInterchangeDeduplication(handlerMap)
        }

        if (features.firstAndLastStopBoardingAlighting) {
            registerBoardingAlighting(handlerMap)
        }

        if (features.emptyGroupOfStopPlacesMembersFilter) {
            registerEmptyGroupOfStopPlacesMembersFilter(handlerMap)
        }

        val plugins =
            buildList {
                if (features.useSalesData) add(salesMatchDataCollector)
                if (features.versionRefNormalizer) add(versionRefNormalizerPlugin)
                if (features.transportModeToLocalScheduledStopPoint) add(transportModeToLocalScheduledStopPointMapper)
                if (features.interchangeDeduplication) add(interchangeCollectorPlugin)
                if (features.firstAndLastStopBoardingAlighting) add(journeyPatternStopPointCollectorPlugin)
                if (features.lineOperatorEnricher) add(lineOperatorEnricher)
                if (features.lineRemovalBasedOnPublicCodes) add(linePublicCodeFilterPlugin)
                if (features.lineRemovalBasedOnTransportMode) add(lineTransportModeFilterPlugin)
            }

        val lineRemovalEnabled =
            features.lineRemovalBasedOnPublicCodes || features.lineRemovalBasedOnTransportMode

        val entitySelectors =
            buildList {
                if (lineRemovalEnabled) add(CascadingLineRemovalSelector(lineFilteringRepository))
                if (features.useSalesData) add(ConvertedServiceJourneyCalendarPruningSelector(salesEnrichmentPlan))
            }

        val skipElements =
            buildList {
                if (features.skipTopographicPlaceCountryRef) {
                    add("/PublicationDelivery/dataObjects/SiteFrame/topographicPlaces/TopographicPlace/CountryRef")
                }
                if (features.skipRequestStop) {
                    add(
                        "/PublicationDelivery/dataObjects/CompositeFrame/frames/ServiceFrame/journeyPatterns/JourneyPattern/pointsInSequence/StopPointInJourneyPattern/RequestStop",
                    )
                    add(
                        "/PublicationDelivery/dataObjects/ServiceFrame/journeyPatterns/JourneyPattern/pointsInSequence/StopPointInJourneyPattern/RequestStop",
                    )
                }
            }

        val unreferencedEntitiesToPrune =
            if (features.pruneUnreferencedEntities) {
                buildSet {
                    // cleans up after line removal
                    if (lineRemovalEnabled) add("DestinationDisplay")
                    // cleans up authorities that do not provide contact details
                    addAll(listOf("Authority", "Operator", "Network"))
                }
            } else {
                emptySet()
            }

        return FilterConfigBuilder()
            .withPlugins(plugins)
            .withEntitySelectors(entitySelectors)
            .withSkipElements(skipElements)
            .withCustomElementHandlers(handlerMap)
            .withRemovePrivateData(false)
            .withPreserveComments(false)
            .withUseSelfClosingTagsWhereApplicable(true)
            .withPruneReferences(false)
            .withUnreferencedEntitiesToPrune(unreferencedEntitiesToPrune)
            .build()
    }

    private fun registerInterchangeDeduplication(handlerMap: MutableMap<String, XMLElementHandler>) {
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

        handlerMap[baseInterchangePath] = interchangeDeduplicationHandler
        // need to handle all child elements of ServiceJourneyInterchange to not leave dangling children
        serviceJourneyInterchangeChildElements.forEach { childElement ->
            handlerMap["$baseInterchangePath/$childElement"] = interchangeDeduplicationHandler
        }
    }

    private fun registerBoardingAlighting(handlerMap: MutableMap<String, XMLElementHandler>) {
        val boardingAlightingHandler =
            StopPointInJourneyPatternBoardingAlightingHandler(journeyPatternStopPointCollectorPlugin)
        listOf(
            "/PublicationDelivery/dataObjects/CompositeFrame/frames/ServiceFrame/journeyPatterns/JourneyPattern",
            "/PublicationDelivery/dataObjects/ServiceFrame/journeyPatterns/JourneyPattern",
        ).forEach { journeyPatternPath ->
            val stopPointPath = "$journeyPatternPath/pointsInSequence/StopPointInJourneyPattern"
            handlerMap[journeyPatternPath] = boardingAlightingHandler
            handlerMap[stopPointPath] = boardingAlightingHandler
            handlerMap["$stopPointPath/${NetexTypes.FOR_ALIGHTING}"] = boardingAlightingHandler
            handlerMap["$stopPointPath/${NetexTypes.FOR_BOARDING}"] = boardingAlightingHandler
        }
    }

    private fun registerEmptyGroupOfStopPlacesMembersFilter(handlerMap: MutableMap<String, XMLElementHandler>) {
        val groupOfStopPlacesMembersPath =
            "/PublicationDelivery/dataObjects/SiteFrame/groupsOfStopPlaces/GroupOfStopPlaces/members"
        val emptyMembersFilterHandler = EmptyMembersFilterHandler()
        handlerMap[groupOfStopPlacesMembersPath] = emptyMembersFilterHandler
        // child elements must use the same handler so that the buffered members start tag is written before them.
        // NOTE: If new child element types are allowed in GOSP members, they MUST be added here,
        // otherwise they will be written without the enclosing members start tag.
        listOf("StopPlaceRef", "TaxiRankRef").forEach { childElement ->
            handlerMap["$groupOfStopPlacesMembersPath/$childElement"] = emptyMembersFilterHandler
        }
    }
}
