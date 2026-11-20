package org.entur.ror.ubelluris.file

import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.ror.ubelluris.utils.LogKeys.PROVIDER
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class LocalFilePublisher(
    private val outputPath: Path,
) : FilePublisher {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(
        stopPlacePath: Path,
        timetablePaths: Map<String, Path>,
    ): Path {
        logger.info("Publishing stop place file: ${stopPlacePath.fileName}")
        val stopsDir = outputPath.resolve(FilePublisher.STOPS_DIR)
        moveIfNeeded(stopPlacePath, stopsDir)

        timetablePaths.forEach { (provider, timetablePath) ->
            logger.info("Publishing timetable for provider: {}", kv(PROVIDER, provider))
            val providerOutDir = outputPath.resolve(FilePublisher.TIMETABLE_DIR).resolve(provider)
            Files.walk(timetablePath).filter(Files::isRegularFile).toList().forEach { file ->
                moveIfNeeded(file, providerOutDir)
            }
        }

        return outputPath
    }

    private fun moveIfNeeded(
        file: Path,
        targetDir: Path,
    ) {
        val target = targetDir.resolve(file.fileName)
        if (file.toAbsolutePath().normalize() == target.toAbsolutePath().normalize()) {
            return
        }
        Files.createDirectories(targetDir)
        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING)
    }
}
