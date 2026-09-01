package org.entur.ror.ubelluris.filter

import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.netex.tools.lib.report.FilterReport
import org.entur.netex.tools.pipeline.app.FilterNetexApp
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.model.TimetableData
import org.entur.ror.ubelluris.sax.enrichment.LineOperatorInserter
import org.entur.ror.ubelluris.utils.LogKeys.PROVIDER
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension

class TimetableFilterService(
    private val cliConfig: CliConfig,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun process(rawTimetableData: Map<String, Path>): Map<String, TimetableData> {
        logger.info("Starting timetable filtering for providers: ${rawTimetableData.keys}")

        return rawTimetableData.mapValues { (provider, path) ->
            filterProvider(provider, path.toFile())
        }
    }

    private fun filterProvider(
        provider: String,
        rawData: File,
    ): TimetableData =
        MDC.putCloseable(PROVIDER, provider).use {
            logger.info("Filtering timetables for provider: {} from: {}", kv(PROVIDER, provider), rawData)

            val resultsDir = Path(cliConfig.resultsDir).resolve(provider)
            Files.createDirectories(resultsDir)

            val filterConfig = TimetableFilterConfig(cliConfig)
            val filterResult =
                FilterNetexApp(
                    filterConfig = filterConfig.build(),
                    input = rawData,
                    target = resultsDir.toFile(),
                ).run()

            // Post-process: enrich Lines with OperatorRef from ServiceJourneys
            val lineOperatorInserter = LineOperatorInserter(filterConfig.lineOperatorEnricher)

            // After removing lines, some PublicationDeliveries may become "empty"
            // This will trigger errors in Antur, so we delete those files from the results directory
            val emptyFiles = filesWithoutServiceJourneys(filterResult)

            emptyFiles
                .filter(::isLineFile)
                .forEach { file ->
                    try {
                        Files.deleteIfExists(file.toPath())
                        logger.info("Deleted empty file: ${file.path}")
                    } catch (e: Exception) {
                        logger.warn("Failed to delete empty file: ${file.path}", e)
                    }
                }

            Files
                .walk(resultsDir)
                .filter { it.extension == "xml" }
                .forEach { xmlFile ->
                    try {
                        lineOperatorInserter.insert(xmlFile)
                    } catch (e: Exception) {
                        logger.warn("Failed to enrich operators for file: ${xmlFile.fileName}", e)
                    }
                }

            val quayModes = filterConfig.transportModeToLocalScheduledStopPointMapper.getCollectedData()

            logger.info("Done processing {}, {} quay mode mappings", kv(PROVIDER, provider), quayModes.size)

            TimetableData(provider, resultsDir, quayModes)
        }

    private fun filesWithoutServiceJourneys(report: FilterReport): Set<File> =
        report.entitiesByFile
            .filter { (_, entities) ->
                entities.none { it.type == "ServiceJourney" } &&
                    entities.none { it.type == "DatedServiceJourney" }
            }.keys

    private fun isLineFile(file: File) = !file.name.startsWith("_")
}
