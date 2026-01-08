package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.lib.config.FilterConfig
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.sax.handlers.CodespaceIdHandler
import org.entur.ror.ubelluris.sax.handlers.ParentStopPlaceAttributeSkipHandler
import org.entur.ror.ubelluris.sax.handlers.PublicCodeFilterHandler
import org.entur.ror.ubelluris.sax.handlers.PublicationTimestampHandler
import org.entur.ror.ubelluris.sax.handlers.SiteFrameHandler
import org.entur.ror.ubelluris.sax.handlers.StopPlaceIdHandler
import org.entur.ror.ubelluris.sax.handlers.StopPlaceParentSiteRefHandler
import org.entur.ror.ubelluris.sax.handlers.StopPlaceQuayHandler
import org.entur.ror.ubelluris.sax.handlers.TextTrimmingHandler
import org.entur.ror.ubelluris.sax.handlers.ValidBetweenFromDateHandler
import org.entur.ror.ubelluris.sax.handlers.XmlnsHandler
import org.entur.ror.ubelluris.sax.handlers.XmlnsUrlHandler
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingPlugin
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.entur.ror.ubelluris.sax.selectors.entities.StopPlacePurgingEntitySelector
import org.entur.ror.ubelluris.sax.selectors.refs.StopPlacePurgingRefSelector

class StandardImportFilterConfig(
    private val cliConfig: CliConfig,
    private val blacklistFilePath: String
) : FilterProfileConfiguration {
    val stopPlacePurgingRepository = StopPlacePurgingRepository()

    override fun build(): FilterConfig {
        val parentStopPlaceAttributeSkipHandler = ParentStopPlaceAttributeSkipHandler(stopPlacePurgingRepository)

        return FilterConfigBuilder()
            .withSkipElements(
                listOf(
                    "/PublicationDelivery/dataObjects/SiteFrame/topographicPlaces",
                    "/PublicationDelivery/dataObjects/SiteFrame/tariffZones",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/ShortName",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/alternativeNames",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/TopographicPlaceRef",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/quays/Quay/Name",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/quays/Quay/ShortName",
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/tariffZones",
                )
            )
            .withCustomElementHandlers(
                mapOf(
                    "/PublicationDelivery/PublicationTimestamp" to PublicationTimestampHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame" to SiteFrameHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/ValidBetween/FromDate" to ValidBetweenFromDateHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/ValidBetween/FromDate" to ValidBetweenFromDateHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/codespaces/Codespace" to CodespaceIdHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/codespaces/Codespace/Xmlns" to XmlnsHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/codespaces/Codespace/XmlnsUrl" to XmlnsUrlHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace" to StopPlaceIdHandler(
                        parentStopPlaceAttributeSkipHandler
                    ),
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/Name" to TextTrimmingHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/ParentSiteRef" to StopPlaceParentSiteRefHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/quays/Quay" to StopPlaceQuayHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/quays/Quay/PublicCode" to PublicCodeFilterHandler(
                        cliConfig.illegalPublicCodes
                    ),
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/quays/Quay/PrivateCode" to TextTrimmingHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/TransportMode" to parentStopPlaceAttributeSkipHandler,
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/StopPlaceType" to parentStopPlaceAttributeSkipHandler,
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/Weighting" to parentStopPlaceAttributeSkipHandler
                )
            )
            .withPlugins(
                listOf(
                    StopPlacePurgingPlugin(stopPlacePurgingRepository, blacklistFilePath)
                )
            )
            .withEntitySelectors(
                listOf(
                    StopPlacePurgingEntitySelector(stopPlacePurgingRepository)
                )
            )
            .withRefSelectors(listOf(StopPlacePurgingRefSelector()))
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
}