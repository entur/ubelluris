package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.pipeline.app.FilterNetexApp
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.model.TimetableData
import org.entur.ror.ubelluris.processor.KeyValueMigrationProcessor
import org.entur.ror.ubelluris.processor.StopPlaceTypeNormalizer
import org.entur.ror.ubelluris.sax.enrichment.QuayModeMatcher
import org.entur.ror.ubelluris.sax.enrichment.StopPlaceAnalyzer
import org.entur.ror.ubelluris.sax.enrichment.StopPlaceSplitter
import org.entur.ror.ubelluris.sax.enrichment.TransportModeInserter
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

class StopPlaceFilterService(
    val cliConfig: CliConfig,
    val blacklistFilePath: String,
) : XmlProcessor {
    private val filterConfig = StandardImportFilterConfig(cliConfig, blacklistFilePath)
    private val resultsDir = Path(cliConfig.resultsDir)
    private val logger = LoggerFactory.getLogger(javaClass)

    private val quayModeMatcher = QuayModeMatcher()
    private val stopPlaceAnalyzer = StopPlaceAnalyzer()
    private val transportModeInserter = TransportModeInserter(StopPlaceSplitter())

    override fun process(
        inputFile: Path,
        timetableData: Map<String, TimetableData>,
    ): Path {
        val outputFile =
            resultsDir.resolve(
                inputFile.fileName.toString().replace(".xml", "_filtered.xml"),
            )
        return filter(inputFile, outputFile, timetableData)
    }

    fun filter(
        inputFile: Path,
        outputFile: Path,
        timetableData: Map<String, TimetableData> = emptyMap(),
    ): Path {
        Files.createDirectories(resultsDir)

        val tempDir = Files.createTempDirectory("ubelluris-filter-")
        logger.info("Temp processing dir: $tempDir")

        val tempInputFile = tempDir.resolve(inputFile.fileName)
        Files.copy(inputFile, tempInputFile, StandardCopyOption.REPLACE_EXISTING)

        if (timetableData.isNotEmpty()) {
            logger.info("Running stop place enrichment from timetable data")
            val aggregatedQuayModes =
                timetableData.values
                    .flatMap { it.quayModes.entries }
                    .groupBy({ it.key }, { it.value })
                    .mapValues { (_, sets) -> sets.flatten().toSet() }

            val quayModeMapping = quayModeMatcher.match(tempInputFile, aggregatedQuayModes)
            val analyses = stopPlaceAnalyzer.analyze(tempInputFile, quayModeMapping)
            transportModeInserter.insert(tempInputFile, analyses)
            logger.info("Done running stop place enrichment")
        }

        Files
            .list(tempDir)
            .filter { it.toString().endsWith(".xml") }
            .forEach { xmlPath ->
                KeyValueMigrationProcessor().process(xmlPath.toFile())
                StopPlaceTypeNormalizer().process(xmlPath.toFile())
            }

        FilterNetexApp(
            filterConfig = filterConfig.build(),
            input = tempDir.toFile(),
            target = tempDir.toFile(),
        ).run()

        val filteredTempFile =
            Files
                .list(tempDir)
                .filter { it.fileName.toString().endsWith(".xml") }
                .max(Comparator.comparing { Files.getLastModifiedTime(it).toMillis() })
                .orElseThrow { IllegalStateException("No filtered file produced.") }

        Files.copy(filteredTempFile, outputFile, StandardCopyOption.REPLACE_EXISTING)
        tempDir.toFile().deleteRecursively()

        return outputFile
    }
}
