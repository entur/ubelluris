package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.file.FileFetchResult
import org.entur.ror.ubelluris.file.FileFetcher
import org.entur.ror.ubelluris.file.FilePublisher
import org.entur.ror.ubelluris.filter.StopPlaceFilterService
import org.entur.ror.ubelluris.filter.TimetableFilterService
import org.slf4j.LoggerFactory
import java.nio.file.Path

class UbellurisService(
    private val fetcher: FileFetcher,
    private val timetableProcessor: TimetableFilterService,
    private val stopPlaceProcessor: StopPlaceFilterService,
    private val publisher: FilePublisher,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun run(): Path {
        logger.info("Staring Ubelluris pipeline...")

        val rawFiles: FileFetchResult = fetcher.fetch()
        logger.info("Fetched files: $rawFiles")

        val processedTimetables = timetableProcessor.process(rawFiles.timetablePaths)
        logger.info("Processed timetables: $processedTimetables")

        val processedStopPlaces = stopPlaceProcessor.process(rawFiles.stopPlacePath, processedTimetables)
        logger.info("Processed stop places: $processedStopPlaces")

        val timetableDirs = processedTimetables.mapValues { (_, data) -> data.providerDir }
        val publishedDir = publisher.publish(processedStopPlaces, timetableDirs)
        logger.info("Published folder: $publishedDir")

        return publishedDir
    }
}
