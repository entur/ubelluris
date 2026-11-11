package org.entur.ror.ubelluris.filter

import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.netex.tools.lib.app.FilterNetexApp
import org.entur.netex.tools.lib.report.FilterReport
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.model.SalesData
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

    fun process(
        rawTimetableData: Map<String, Path>,
        salesData: SalesData,
    ): Map<String, TimetableData> {
        logger.info("Starting timetable filtering for providers: ${rawTimetableData.keys}")

        return rawTimetableData.mapValues { (provider, path) ->
            filterProvider(provider, path.toFile(), salesData)
        }
    }

    private fun filterProvider(
        provider: String,
        rawData: File,
        salesData: SalesData,
    ): TimetableData =
        MDC.putCloseable(PROVIDER, provider).use {
            logger.info("Filtering timetables for provider: {} from: {}", kv(PROVIDER, provider), rawData)

            val resultsDir = Path(cliConfig.resultsDir).resolve(provider)
            Files.createDirectories(resultsDir)

            val salesPatchCarriers = cliConfig.salesPatchCarriers(provider)
            val salesPatchData = salesData.forCarriers(salesPatchCarriers)
            if (salesPatchCarriers.isEmpty()) {
                logger.info("No sales patch configured for {}", kv(PROVIDER, provider))
            } else if (salesPatchData.trainNrServiceJourneysMap.isEmpty()) {
                logger.warn("Sales data has no service journeys for the carriers {} of {}", salesPatchCarriers, kv(PROVIDER, provider))
            }
            val filterConfig = TimetableFilterConfig(cliConfig, salesPatchData)
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

            val salesEnrichmentPlan = filterConfig.salesEnrichmentPlan
            logger.info(
                "Sales match summary for {}: {}. Converted {} service journeys to dated service journeys on {} operating days",
                kv(PROVIDER, provider),
                salesEnrichmentPlan.matchSummary,
                salesEnrichmentPlan.convertedServiceJourneyIds.size,
                salesEnrichmentPlan.operatingDays.size,
            )

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
