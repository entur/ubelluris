package org.entur.ror.ubelluris.filter

import org.entur.netex.tools.pipeline.app.FilterNetexApp
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.timetable.model.TimetableData
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class TimetableNetexProcessor(
    private val cliConfig: CliConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun process(rawTimetableData: Map<String, Path>): Map<String, TimetableData> {
        logger.info("Starting timetable filtering for providers: ${rawTimetableData.keys}")

        return rawTimetableData.mapValues { (provider, path) ->
            filterProvider(provider, path.toFile())
        }
    }

    private fun filterProvider(provider: String, rawData: File): TimetableData {
        logger.info("Filtering timetables for provider: $provider  from: $rawData")

        val resultsDir = Path(cliConfig.resultsDir).resolve(provider)
        Files.createDirectories(resultsDir)

        val filterConfig = TimetableFilterConfig(cliConfig)
        FilterNetexApp(
            filterConfig = filterConfig.build(),
            input = rawData,
            target = resultsDir.toFile()
        ).run()

        val quayModes = filterConfig.plugin.getCollectedData()
        val extractedFiles = listXmlFiles(resultsDir)

        logger.info("Extracted ${extractedFiles.size} files, ${quayModes.size} quay mode mappings")

        return TimetableData(provider, resultsDir, emptyList(), extractedFiles, quayModes)
    }

    private fun listXmlFiles(resultsDir: Path): List<Path> = Files.list(resultsDir)
        .filter { it.toString().endsWith(".xml") }
        .toList()
}
