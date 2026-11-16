package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.lib.NetexProcessor
import org.entur.netex.tools.lib.config.FilterConfig
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.SalesData
import org.entur.ror.ubelluris.model.SalesServiceJourney
import org.entur.ror.ubelluris.sales.TimetableRailServiceJourney
import org.entur.ror.ubelluris.sax.plugins.KeyValueCollectorPlugin
import org.entur.ror.ubelluris.sax.plugins.SalesMatchDataCollector
import org.entur.ror.ubelluris.sax.plugins.SalesMatchRepository
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Collects the sales data from the NeTEx files of the sales directory: line files and _shared_data.
 * It only builds the entity model, because the plugins collect all the data. It does not export files.
 */
class SalesFilterConfig : FilterProfileConfiguration {
    companion object {
        private const val PRODUCT_CODE_KEY = "productCode"
        private const val SALES_AUTHORITY_NUMBER_KEY = "salesAuthorityNumber"

        /** Large subtrees that the sales data does not use. Skipping them keeps the entity model small. */
        private val SKIPPED_ELEMENTS =
            listOf(
                // Line files
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ServiceFrame/journeyPatterns/JourneyPattern/pointsInSequence",
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ServiceFrame/routes/Route/pointsInSequence",
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/vehicleJourneys/ServiceJourney/passingTimes",
                // Shared file
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ResourceFrame",
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ServiceFrame/Network",
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ServiceFrame/additionalNetworks",
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ServiceFrame/routePoints",
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ServiceFrame/destinationDisplays",
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ServiceFrame/scheduledStopPoints",
                "/PublicationDelivery/dataObjects/CompositeFrame/frames/ServiceFrame/stopAssignments",
                // Stops file
                "/PublicationDelivery/dataObjects/SiteFrame",
            )
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    private val salesMatchRepository = SalesMatchRepository()
    private val salesMatchDataCollector = SalesMatchDataCollector(salesMatchRepository)
    private val keyValueCollectorPlugin =
        KeyValueCollectorPlugin(
            mapOf(
                NetexTypes.LINE to setOf(SALES_AUTHORITY_NUMBER_KEY),
                NetexTypes.SERVICE_JOURNEY to setOf(PRODUCT_CODE_KEY),
            ),
        )

    /** Collects the sales data from the NeTEx files in [salesDir]. Groups the rail ServiceJourneys by train number. */
    fun run(salesDir: File): SalesData {
        NetexProcessor(filterConfig = build()).buildEntityModel(salesDir)
        return SalesData(
            trainNrServiceJourneysMap =
                salesMatchRepository
                    .railServiceJourneys()
                    .mapNotNull(::toSalesServiceJourney)
                    .groupBy { it.trainNr },
            calendarData = salesMatchRepository.calendar(),
        )
    }

    override fun build(): FilterConfig =
        FilterConfigBuilder()
            .withPlugins(listOf(salesMatchDataCollector, keyValueCollectorPlugin))
            .withSkipElements(SKIPPED_ELEMENTS)
            .build()

    private fun toSalesServiceJourney(railServiceJourney: TimetableRailServiceJourney): SalesServiceJourney? {
        val serviceJourneyId = railServiceJourney.serviceJourneyId

        // train number is the mapping key, skip the service journey if we cannot get the trainNr
        val trainNr =
            when (railServiceJourney.trainNrs.size) {
                0 -> {
                    logger.warn("service journey {} with rail transport mode has no train number, skipping", serviceJourneyId)
                    return null
                }
                1 -> railServiceJourney.trainNrs.single()
                else -> {
                    logger.warn(
                        "Only supports zero or one train number, but found {}. Skipping sales for service journey {}.",
                        railServiceJourney.trainNrs.size,
                        serviceJourneyId,
                    )
                    return null
                }
            }

        val productCode = keyValueCollectorPlugin.keyValueOrNull(serviceJourneyId, PRODUCT_CODE_KEY)
        if (productCode.isNullOrBlank()) {
            logger.warn("service journey {} has no productCode, skipping", serviceJourneyId)
            return null
        }

        // The repository fails on unresolved Lines, so the Line always resolves here
        val lineId = checkNotNull(salesMatchRepository.lineId(serviceJourneyId))
        val salesAuthorityNumber = keyValueCollectorPlugin.keyValueOrNull(lineId, SALES_AUTHORITY_NUMBER_KEY)
        if (salesAuthorityNumber.isNullOrBlank()) {
            logger.warn("service journey {} has no salesAuthorityNumber on line {}, skipping", serviceJourneyId, lineId)
            return null
        }

        val dayTypeRefs = railServiceJourney.dayTypeRefs.ifEmpty { error("Missing DayTypeRef in service journey $serviceJourneyId") }

        return SalesServiceJourney(
            serviceJourneyId = serviceJourneyId,
            productCode = productCode,
            salesAuthorityNumber = salesAuthorityNumber,
            dayTypeRefs = dayTypeRefs,
            trainNr = trainNr,
        )
    }
}
