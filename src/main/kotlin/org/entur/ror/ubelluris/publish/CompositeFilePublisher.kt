package org.entur.ror.ubelluris.publish

import org.slf4j.LoggerFactory
import java.nio.file.Path

class CompositeFilePublisher(
    private val publishers: List<FilePublisher>
) : FilePublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(file: Path): Path {
        require(publishers.isNotEmpty()) { "At least one publisher required" }

        var result: Path = file
        for (publisher in publishers) {
            logger.info("Publishing with ${publisher.javaClass.simpleName}")
            result = publisher.publish(file)
        }
        return result
    }
}
