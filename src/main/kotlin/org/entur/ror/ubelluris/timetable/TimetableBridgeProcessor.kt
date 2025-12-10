package org.entur.ror.ubelluris.timetable

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.entur.ror.ubelluris.timetable.discovery.QuayModeMatcher
import org.entur.ror.ubelluris.timetable.discovery.ScheduledStopPointExtractor
import org.entur.ror.ubelluris.timetable.fetch.TimetableFetcher
import org.entur.ror.ubelluris.timetable.insertion.StopPlaceAnalyzer
import org.entur.ror.ubelluris.timetable.insertion.StopPlaceSplitter
import org.entur.ror.ubelluris.timetable.insertion.TransportModeInserter
import org.entur.ror.ubelluris.timetable.model.ModeInsertionLog
import org.entur.ror.ubelluris.timetable.model.QuayModeMapping
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Main processor for TransportMode Bridge (VT) functionality
 * Orchestrates VT-1 (discovery) and VT-2 (insertion) phases
 */
class TimetableBridgeProcessor(
    private val timetableFetcher: TimetableFetcher,
    private val config: TimetableConfig
) {

    private val logger = LoggerFactory.getLogger(TimetableBridgeProcessor::class.java)
    private val objectMapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    private val scheduledStopPointExtractor = ScheduledStopPointExtractor()
    private val quayModeMatcher = QuayModeMatcher()
    private val stopPlaceAnalyzer = StopPlaceAnalyzer()
    private val stopPlaceSplitter = StopPlaceSplitter()
    private val transportModeInserter = TransportModeInserter(stopPlaceSplitter)

    /**
     * Processes stops XML to insert correct TransportModes based on timetable data
     *
     * @param stopsXmlPath Path to the stops XML file
     * @return Path to the modified stops XML file
     */
    fun process(stopsXmlPath: Path): Path {
        logger.info("Starting TimetableBridge processing for: $stopsXmlPath")

        try {
            // Phase 1: Fetch timetables
            val timetableDataMap = timetableFetcher.fetch(config.providers)

            // Phase 2: VT-1 Discovery
            val quayModeMapping = performDiscovery(stopsXmlPath, timetableDataMap)

            // Phase 3: VT-2 Insertion
            val (processedPath, insertionLogs) = performInsertion(stopsXmlPath, quayModeMapping)

            // Log results
            logResults(quayModeMapping, insertionLogs)

            logger.info("TimetableBridge processing complete")
            return processedPath

        } catch (e: Exception) {
            logger.error("TimetableBridge processing failed", e)
            throw e
        }
    }

    /**
     * VT-1: Discovery phase - extracts refs from timetables and matches to quays
     */
    private fun performDiscovery(
        stopsXmlPath: Path,
        timetableDataMap: Map<String, org.entur.ror.ubelluris.timetable.model.TimetableData>
    ): QuayModeMapping {
        logger.info("=== VT-1: Discovery Phase ===")

        // Extract ScheduledStopPointRefs from timetables
        val scheduledStopPointRefs = scheduledStopPointExtractor.extract(timetableDataMap)

        // Match refs to quays in stops XML
        val quayModeMapping = quayModeMatcher.match(stopsXmlPath, scheduledStopPointRefs)

        logger.info("Discovery complete: ${quayModeMapping.quayToModes.size} quays matched, ${quayModeMapping.issues.size} issues")

        return quayModeMapping
    }

    /**
     * VT-2: Insertion phase - analyzes StopPlaces and inserts TransportModes
     */
    private fun performInsertion(
        stopsXmlPath: Path,
        quayModeMapping: QuayModeMapping
    ): Pair<Path, List<ModeInsertionLog>> {
        logger.info("=== VT-2: Insertion Phase ===")

        // Analyze StopPlaces to determine scenarios
        val analyses = stopPlaceAnalyzer.analyze(stopsXmlPath, quayModeMapping)

        // Insert TransportModes
        val (processedPath, insertionLogs) = transportModeInserter.insert(stopsXmlPath, analyses)

        logger.info("Insertion complete: ${insertionLogs.size} modifications")

        return Pair(processedPath, insertionLogs)
    }

    /**
     * Logs results to analysis directory
     */
    private fun logResults(quayModeMapping: QuayModeMapping, insertionLogs: List<ModeInsertionLog>) {
        val analysisDir = Path.of("analysis")
        Files.createDirectories(analysisDir)

        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        // Log VT-1 issues
        if (quayModeMapping.issues.isNotEmpty()) {
            val issuesFile = analysisDir.resolve("${today}_vt-quay-mode-issues.json")
            Files.newBufferedWriter(issuesFile).use { writer ->
                objectMapper.writeValue(writer, quayModeMapping.issues)
            }
            logger.info("Wrote VT-1 issues to: $issuesFile")
        }

        // Log VT-2 insertions
        if (insertionLogs.isNotEmpty()) {
            val insertionFile = analysisDir.resolve("${today}_vt_mode_insertion_log.json")
            Files.newBufferedWriter(insertionFile).use { writer ->
                objectMapper.writeValue(writer, insertionLogs)
            }
            logger.info("Wrote VT-2 insertion log to: $insertionFile")
        }
    }
}
