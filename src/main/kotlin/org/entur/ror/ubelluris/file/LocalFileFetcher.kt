package org.entur.ror.ubelluris.file

import java.nio.file.Files
import java.nio.file.Path

class LocalFileFetcher(
    private val stopPlacePath: String,
    private val timetablePaths: Map<String, String>,
    private val salesBlobPath: String,
    private val extractDir: Path = Files.createTempDirectory("ubelluris-input"),
) : FileFetcher {
    override fun fetch(): FileFetchResult =
        FileFetchResult(
            stopPlacePath = extractStopPlace(Path.of(stopPlacePath)),
            timetablePaths = timetablePaths.mapValues { (provider, path) -> extractTimetable(provider, Path.of(path)) },
            salesPath = extractSales(Path.of(salesBlobPath)),
        )

    private fun extractStopPlace(path: Path): Path =
        if (path.isZip()) {
            extractXmlFromZip(path, extractDir.resolve("stops").resolve("sweden_stop_places.xml"))
        } else {
            path
        }

    private fun extractSales(path: Path): Path =
        if (path.isZip()) {
            extractZipToDirectory(path, extractDir.resolve("sales"))
        } else {
            path
        }

    private fun extractTimetable(
        provider: String,
        path: Path,
    ): Path {
        val resolvedPath = path.withDirectoryFallback()
        return if (resolvedPath.isZip()) {
            val outputDir = Files.createDirectories(extractDir.resolve("timetable").resolve(provider))
            extractZipToDirectory(resolvedPath, outputDir)
        } else {
            resolvedPath
        }
    }

    private fun Path.withDirectoryFallback(): Path {
        if (!isZip() || Files.exists(this)) return this
        val directory = resolveSibling(fileName.toString().dropLast(ZIP_SUFFIX.length))
        return if (Files.isDirectory(directory)) directory else this
    }

    private fun Path.isZip(): Boolean = fileName.toString().endsWith(ZIP_SUFFIX, ignoreCase = true)

    companion object {
        private const val ZIP_SUFFIX = ".zip"
    }
}
