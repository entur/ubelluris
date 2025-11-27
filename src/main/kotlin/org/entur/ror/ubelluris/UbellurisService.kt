package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.file.FileFetcher
import org.entur.ror.ubelluris.filter.XmlProcessor
import org.entur.ror.ubelluris.publish.FilePublisher
import org.slf4j.LoggerFactory
import java.nio.file.Path

class UbellurisService(
    private val fetcher: FileFetcher,
    private val processor: XmlProcessor,
    private val publisher: FilePublisher
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun run(): Path {
        logger.info("Staring Ubelluris pipeline...")

        val rawFile: Path = fetcher.fetch()
        logger.info("Fetched file: $rawFile")

        val processedFile: Path = processor.process(rawFile)
        logger.info("Processed file: $processedFile")

        val publishedFile: Path = publisher.publish(processedFile)
        logger.info("Published file: $publishedFile")

        return publishedFile
    }
}