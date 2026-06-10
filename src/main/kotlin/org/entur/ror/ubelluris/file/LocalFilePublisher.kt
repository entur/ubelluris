package org.entur.ror.ubelluris.file

import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.ror.ubelluris.utils.LogKeys.PROVIDER
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class LocalFilePublisher(
    private val storagePath: Path,
    private val resultsDir: Path,
) : FilePublisher {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(
        stopPlacePath: Path,
        timetablePaths: Map<String, Path>,
    ): Path {
        logger.info("Publishing stop place file: ${stopPlacePath.fileName}")
        val stopsDir = resultsDir.resolve(storagePath).resolve(FilePublisher.STOPS_DIR)
        Files.createDirectories(stopsDir)
        Files.move(stopPlacePath, stopsDir.resolve(stopPlacePath.fileName), StandardCopyOption.REPLACE_EXISTING)

        timetablePaths.forEach { (provider, timetablePath) ->
            logger.info("Publishing timetable for provider: {}", kv(PROVIDER, provider))
            val providerOutDir = resultsDir.resolve(storagePath).resolve(FilePublisher.TIMETABLE_DIR).resolve(provider)
            Files.createDirectories(providerOutDir)
            Files.walk(timetablePath).filter(Files::isRegularFile).forEach { file ->
                Files.move(file, providerOutDir.resolve(file.fileName), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        return resultsDir
    }
}
