package org.entur.ror.ubelluris.file

import java.nio.file.Files
import java.nio.file.Path

class LocalFileFetcher(
    private val stopPlacePath: String,
    private val timetablePaths: Map<String, String>,
    private val extractDir: Path = Files.createTempDirectory("ubelluris-input"),
) : FileFetcher {
    override fun fetch(): FileFetchResult =
        FileFetchResult(
            stopPlacePath = extractStopPlace(Path.of(stopPlacePath)),
            timetablePaths = timetablePaths.mapValues { (provider, path) -> extractTimetable(provider, Path.of(path)) },
        )

    private fun extractStopPlace(path: Path): Path =
        if (path.isZip()) {
            extractXmlFromZip(path, extractDir.resolve("stops").resolve("sweden_stop_places.xml"))
        } else {
            path
        }

    private fun extractTimetable(
        provider: String,
        path: Path,
    ): Path =
        if (path.isZip()) {
            val outputDir = Files.createDirectories(extractDir.resolve("timetable").resolve(provider))
            extractZipToDirectory(path, outputDir)
        } else {
            path
        }

    private fun Path.isZip(): Boolean = fileName.toString().endsWith(".zip", ignoreCase = true)
}
