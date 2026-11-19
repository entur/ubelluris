package org.entur.ror.ubelluris.sales

import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.filter.SalesFilterConfig
import org.entur.ror.ubelluris.model.SalesData
import org.slf4j.LoggerFactory
import java.nio.file.Path

class SalesDataService(
    private val cliConfig: CliConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** [salesDir] is null when the sales data is not fetched, because the feature useSalesData is disabled. */
    fun process(salesDir: Path?): SalesData {
        if (!cliConfig.timetableFilterFeatures.useSalesData) {
            logger.info("Feature useSalesData is disabled, skipping sales data")
            return SalesData.EMPTY
        }
        requireNotNull(salesDir) { "Feature useSalesData is enabled, but no sales data was fetched" }

        logger.info("Processing sales data from {}", salesDir)
        val salesData = SalesFilterConfig().run(salesDir.toFile())

        val calendarData = salesData.calendarData
        logger.info(
            "Collected sales shared data: {} day types, {} operating periods, {} day type assignments, {} date assignments",
            calendarData.dayTypes.size,
            calendarData.operatingPeriods.size,
            calendarData.operatingPeriodIdsByDayTypeId.values.sumOf { it.size },
            calendarData.dayTypeDateAssignments.values.sumOf { it.size },
        )
        logger.info("Collected sales ServiceJourneys for {} train numbers", salesData.trainNrServiceJourneysMap.size)
        return salesData
    }
}
