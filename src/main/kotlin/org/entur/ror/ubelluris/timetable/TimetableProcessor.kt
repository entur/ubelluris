package org.entur.ror.ubelluris.timetable

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
import java.nio.file.Path

class TimetableProcessor(
    private val timetableFetcher: TimetableFetcher,
    private val config: TimetableConfig
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val scheduledStopPointExtractor = ScheduledStopPointExtractor()
    private val quayModeMatcher = QuayModeMatcher()
    private val stopPlaceAnalyzer = StopPlaceAnalyzer()
    private val stopPlaceSplitter = StopPlaceSplitter()
    private val transportModeInserter = TransportModeInserter(stopPlaceSplitter)

    fun process(stopsXmlPath: Path): Path {
        logger.info("Starting timetable processing for: $stopsXmlPath")

        try {
            val timetableDataMap = timetableFetcher.fetch(config.providers)

            val quayModeMapping = performDiscovery(stopsXmlPath, timetableDataMap)

            val (processedPath, insertionLogs) = performInsertion(stopsXmlPath, quayModeMapping)

            logger.info("Timetable processing complete")
            return processedPath

        } catch (e: Exception) {
            logger.error("Timetable processing failed", e)
            throw e
        }
    }

    private fun performDiscovery(
        stopsXmlPath: Path,
        timetableDataMap: Map<String, org.entur.ror.ubelluris.timetable.model.TimetableData>
    ): QuayModeMapping {
        logger.info("Extracting refs from timetables and match quays")

        val scheduledStopPointRefs = scheduledStopPointExtractor.extract(timetableDataMap)

        val quayModeMapping = quayModeMatcher.match(stopsXmlPath, scheduledStopPointRefs)

        logger.info("Done extracting refs from timetables, ${quayModeMapping.quayToModes.size} quays matched")

        return quayModeMapping
    }

    private fun performInsertion(
        stopsXmlPath: Path,
        quayModeMapping: QuayModeMapping
    ): Pair<Path, List<ModeInsertionLog>> {
        logger.info("Analysing stop places and insert transport modes")

        val analyses = stopPlaceAnalyzer.analyze(stopsXmlPath, quayModeMapping)

        val (processedPath, insertionLogs) = transportModeInserter.insert(stopsXmlPath, analyses)

        logger.info("Done analysing stop places, ${insertionLogs.size} modifications")

        return Pair(processedPath, insertionLogs)
    }
}
