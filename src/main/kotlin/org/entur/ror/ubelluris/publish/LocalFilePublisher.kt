package org.entur.ror.ubelluris.publish

import org.entur.ror.ubelluris.publish.FilePublisher.Companion.STOPS_DIR
import org.entur.ror.ubelluris.publish.FilePublisher.Companion.TIMETABLE_DIR
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class LocalFilePublisher(
    private val storagePath: Path,
    private val resultsDir: Path
) : FilePublisher {

    override fun publish(stopPlacePath: Path, timetablePaths: Map<String, Path>): Path {
        val stopsDir = resultsDir.resolve(STOPS_DIR)
        Files.createDirectories(stopsDir)
        Files.move(stopPlacePath, stopsDir.resolve(stopPlacePath.fileName), StandardCopyOption.REPLACE_EXISTING)

        timetablePaths.forEach { (provider, timetablePath) ->
            val providerOutDir = resultsDir.resolve(storagePath).resolve(TIMETABLE_DIR).resolve(provider)
            Files.createDirectories(providerOutDir)
            Files.walk(timetablePath).filter(Files::isRegularFile).forEach { file ->
                Files.move(file, providerOutDir.resolve(file.fileName), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        return resultsDir
    }
}
