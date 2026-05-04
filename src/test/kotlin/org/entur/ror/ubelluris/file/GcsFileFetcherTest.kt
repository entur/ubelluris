package org.entur.ror.ubelluris.file

import com.google.cloud.storage.Storage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GcsFileFetcherTest {

    @TempDir
    lateinit var tempDir: Path

    private val storage: Storage = mock()

    @Test
    fun shouldExtractStopPlaceXmlFromZip() {
        val stopPlaceBlobPath = "stops/stop_places.zip"
        val stopsDir = tempDir.resolve("stops")
        Files.createDirectories(stopsDir)
        createZipFile(stopsDir.resolve("stop_places.zip"), mapOf("stops.xml" to "<StopPlaces/>"))

        val fetcher = GcsFileFetcher(
            storage = storage,
            inputBucketName = "test-bucket",
            stopPlaceBlobPath = stopPlaceBlobPath,
            timetableBlobPaths = emptyMap(),
            downloadDir = tempDir
        )

        val result = fetcher.fetch()

        assertThat(result.stopPlacePath).exists()
        assertThat(result.stopPlacePath.fileName.toString()).isEqualTo("sweden_stop_places.xml")
        assertThat(Files.readString(result.stopPlacePath)).isEqualTo("<StopPlaces/>")
    }

    @Test
    fun shouldExtractMultipleTimetableZipsToSeparateDirectories() {
        val stopPlaceBlobPath = "stops/stop_places.zip"
        val stopsDir = tempDir.resolve("stops")
        Files.createDirectories(stopsDir)
        createZipFile(stopsDir.resolve("stop_places.zip"), mapOf("stops.xml" to "<StopPlaces/>"))

        val timetableDir = tempDir.resolve("timetable")
        Files.createDirectories(timetableDir)
        createZipFile(
            timetableDir.resolve("provider1.zip"),
            mapOf("line_001.xml" to "<Line/>", "line_002.xml" to "<Line/>")
        )
        createZipFile(
            timetableDir.resolve("provider2.zip"),
            mapOf("line_001.xml" to "<Line/>")
        )

        val fetcher = GcsFileFetcher(
            storage = storage,
            inputBucketName = "test-bucket",
            stopPlaceBlobPath = stopPlaceBlobPath,
            timetableBlobPaths = mapOf(
                "provider1" to "timetable/provider1.zip",
                "provider2" to "timetable/provider2.zip"
            ),
            downloadDir = tempDir
        )

        val result = fetcher.fetch()

        val provider1Dir = result.timetablePaths["provider1"]!!
        assertThat(provider1Dir).isDirectory()
        assertThat(Files.list(provider1Dir).toList())
            .extracting<String> { it.fileName.toString() }
            .containsExactlyInAnyOrder("line_001.xml", "line_002.xml")

        val provider2Dir = result.timetablePaths["provider2"]!!
        assertThat(provider2Dir).isDirectory()
        assertThat(Files.list(provider2Dir).toList())
            .extracting<String> { it.fileName.toString() }
            .containsExactly("line_001.xml")
    }

    @Test
    fun shouldSkipDownloadWhenFilesAlreadyCached() {
        val stopPlaceBlobPath = "stops/stop_places.zip"
        val stopsDir = tempDir.resolve("stops")
        Files.createDirectories(stopsDir)
        createZipFile(stopsDir.resolve("stop_places.zip"), mapOf("stops.xml" to "<StopPlaces/>"))

        val fetcher = GcsFileFetcher(
            storage = storage,
            inputBucketName = "test-bucket",
            stopPlaceBlobPath = stopPlaceBlobPath,
            timetableBlobPaths = emptyMap(),
            downloadDir = tempDir
        )

        fetcher.fetch()
        val secondResult = fetcher.fetch()

        assertThat(secondResult.stopPlacePath).exists()
    }

    private fun createZipFile(path: Path, files: Map<String, String>) {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        Files.write(path, baos.toByteArray())
    }
}
