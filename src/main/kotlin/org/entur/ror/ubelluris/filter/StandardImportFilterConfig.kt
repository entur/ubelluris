package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.lib.config.FilterConfig
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.ror.ubelluris.sax.handlers.StopPlaceIdHandler
import org.entur.ror.ubelluris.sax.handlers.StopPlaceParentSiteRefHandler
import org.entur.ror.ubelluris.sax.handlers.StopPlaceQuayHandler
import org.entur.ror.ubelluris.sax.plugins.PublicCodePlugin
import org.entur.ror.ubelluris.sax.plugins.PublicCodeRepository
import org.entur.ror.ubelluris.sax.selectors.entities.PublicCodeSelector
import org.entur.ror.ubelluris.sax.selectors.refs.PublicCodeRefSelector

class StandardImportFilterConfig : FilterProfileConfiguration {
    val publicCodeRepository = PublicCodeRepository()
    override fun build(): FilterConfig =
        FilterConfigBuilder()
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
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace" to StopPlaceIdHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/ParentSiteRef" to StopPlaceParentSiteRefHandler(),
                    "/PublicationDelivery/dataObjects/SiteFrame/stopPlaces/StopPlace/quays/Quay" to StopPlaceQuayHandler()
                )
            )
            .withPlugins(listOf(PublicCodePlugin(publicCodeRepository)))
            .withEntitySelectors(listOf(PublicCodeSelector(publicCodeRepository)))
            .withRefSelectors(listOf(PublicCodeRefSelector()))
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