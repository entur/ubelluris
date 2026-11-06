package org.entur.ror.ubelluris.sales

import org.entur.ror.ubelluris.model.SalesData
import org.entur.ror.ubelluris.model.SalesServiceJourney
import org.entur.ror.ubelluris.model.SalesSharedData
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

class SalesDataService(
    private val lineFileParser: SalesLineFileParser = SalesLineFileParser(),
    private val sharedDataParser: SalesSharedDataParser = SalesSharedDataParser(),
) {
    companion object {
        private const val SHARED_DATA_FILE_NAME = "_shared_data.xml"
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    fun process(salesDir: Path): SalesData =
        SalesData(
            trainNrServiceJourneysMap = processLineFiles(salesDir),
            sharedData = processSharedData(salesDir),
        )

    fun processSharedData(salesDir: Path): SalesSharedData {
        val sharedDataPath = salesDir.resolve(SHARED_DATA_FILE_NAME)
        logger.info("Processing sales shared data from {}", sharedDataPath)

        val sharedData = sharedDataParser.parse(sharedDataPath)

        logger.info(
            "Collected sales shared data: {} day types, {} operating periods, {} day type assignments, {} date assignments",
            sharedData.dayTypes.size,
            sharedData.operatingPeriods.size,
            sharedData.dayTypeAssignments.size,
            sharedData.dayTypeDateAssignments.values.sumOf { it.size },
        )
        return sharedData
    }

    /** Groups the ServiceJourneys of all line files in [salesDir] by train number. */
    fun processLineFiles(salesDir: Path): Map<String, List<SalesServiceJourney>> {
        val lineFilePaths =
            Files.list(salesDir).use { files ->
                files.filter(::isLineFile).sorted().toList()
            }
        logger.info("Processing {} sales line files from {}", lineFilePaths.size, salesDir)

        val trainNrServiceJourneysMap =
            lineFilePaths
                .flatMap { lineFilePath ->
                    try {
                        lineFileParser.parse(lineFilePath)
                    } catch (exception: Exception) {
                        logger.warn("Failed to parse sales line file ${lineFilePath.name}", exception)
                        emptyList()
                    }
                }.groupBy { it.trainNr }

        logger.info("Collected sales ServiceJourneys for {} train numbers", trainNrServiceJourneysMap.size)
        return trainNrServiceJourneysMap
    }

    private fun isLineFile(path: Path) = path.extension == "xml" && !path.name.startsWith("_")
}
