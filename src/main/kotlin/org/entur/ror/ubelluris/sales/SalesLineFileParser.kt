package org.entur.ror.ubelluris.sales

import org.entur.ror.ubelluris.model.SalesServiceJourney
import org.entur.ror.ubelluris.utils.NetexLineFile
import org.slf4j.LoggerFactory
import java.nio.file.Path

class SalesLineFileParser {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun parse(lineFilePath: Path): List<SalesServiceJourney> {
        val lineFile = NetexLineFile.read(lineFilePath)

        return lineFile.serviceJourneys
            .mapNotNull { serviceJourney ->
                val line = lineFile.resolveLine(serviceJourney)
                if (lineFile.resolveTransportMode(serviceJourney, line) != NetexLineFile.RAIL_TRANSPORT_MODE) {
                    return@mapNotNull null
                }

                val trainNr = lineFile.resolveTrainNr(serviceJourney)
                // train number is the mapping key, skip the service journey if we cannot get the trainNr
                if (trainNr.isNullOrBlank()) {
                    logger.warn("service journey {} with rail transport mode has no train number, skipping", serviceJourney.id)
                    return@mapNotNull null
                }

                val productCode = serviceJourney.keyValueOrNull("productCode")
                if (productCode.isNullOrBlank()) {
                    logger.warn("service journey {} has no productCode, skipping", serviceJourney.id)
                    return@mapNotNull null
                }

                val salesAuthorityNumber = line.keyValueOrNull("salesAuthorityNumber")
                if (salesAuthorityNumber.isNullOrBlank()) {
                    logger.warn("service journey {} has no salesAuthorityNumber on line {}, skipping", serviceJourney.id, line.id)
                    return@mapNotNull null
                }

                SalesServiceJourney(
                    serviceJourneyId = serviceJourney.id,
                    productCode = productCode,
                    salesAuthorityNumber = salesAuthorityNumber,
                    dayTypeRefs = serviceJourney.requireValues("dayTypes/DayTypeRef"),
                    trainNr = trainNr,
                )
            }
    }
}
