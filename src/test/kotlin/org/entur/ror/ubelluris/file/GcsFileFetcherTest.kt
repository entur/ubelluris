package org.entur.ror.ubelluris.file

import com.google.cloud.storage.Storage
import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.config.GcsConfig
import org.junit.jupiter.api.BeforeEach
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
    private val config =
        GcsConfig(
            projectId = "",
            outputBucketName = "",
            inputBucketName = "test-bucket",
            uploadEnabled = false,
            downloadEnabled = true,
        )

    private val stopPlaceBlobPath = "stops/stop_places.zip"
    private val salesBlobPath = "sales/samtrafikensales_latest.zip"

    @BeforeEach
    fun createCachedStopPlaceAndSalesZips() {
        val stopsDir = tempDir.resolve("stops")
        Files.createDirectories(stopsDir)
        createZipFile(stopsDir.resolve("stop_places.zip"), mapOf("stops.xml" to "<StopPlaces/>"))

        val salesDir = tempDir.resolve("sales")
        Files.createDirectories(salesDir)
        createZipFile(
            salesDir.resolve("samtrafikensales_latest.zip"),
            mapOf("sales_001.xml" to "<FareFrame/>", "sales_002.xml" to "<FareFrame/>"),
        )
    }

    @Test
    fun shouldExtractStopPlaceXmlFromZip() {
        val result = createFetcher().fetch()

        assertThat(result.stopPlacePath).exists()
        assertThat(result.stopPlacePath.fileName.toString()).isEqualTo("sweden_stop_places.xml")
        assertThat(Files.readString(result.stopPlacePath)).isEqualTo("<StopPlaces/>")
    }

    @Test
    fun shouldExtractMultipleTimetableZipsToSeparateDirectories() {
        val timetableDir = tempDir.resolve("timetable")
        Files.createDirectories(timetableDir)
        createZipFile(
            timetableDir.resolve("provider1.zip"),
            mapOf("line_001.xml" to "<Line/>", "line_002.xml" to "<Line/>"),
        )
        createZipFile(
            timetableDir.resolve("provider2.zip"),
            mapOf("line_001.xml" to "<Line/>"),
        )

        val result =
            createFetcher(
                timetableBlobPaths =
                    mapOf(
                        "provider1" to "timetable/provider1.zip",
                        "provider2" to "timetable/provider2.zip",
                    ),
            ).fetch()

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
    fun shouldExtractSalesZipIntoSalesDirectory() {
        val result = createFetcher().fetch()

        assertThat(result.salesPath).isEqualTo(tempDir.resolve("sales"))
        assertThat(Files.list(result.salesPath!!).toList())
            .extracting<String> { it.fileName.toString() }
            .containsExactlyInAnyOrder("samtrafikensales_latest.zip", "sales_001.xml", "sales_002.xml")
    }

    @Test
    fun shouldSkipDownloadWhenFilesAlreadyCached() {
        val fetcher = createFetcher()

        fetcher.fetch()
        val secondResult = fetcher.fetch()

        assertThat(secondResult.stopPlacePath).exists()
        assertThat(secondResult.salesPath!!.resolve("sales_001.xml")).exists()
    }

    @Test
    fun shouldNotFetchSalesDataWithoutSalesBlobPath() {
        val result = createFetcher(salesBlobPath = null).fetch()

        assertThat(result.salesPath).isNull()
        assertThat(Files.list(tempDir.resolve("sales")).toList())
            .extracting<String> { it.fileName.toString() }
            .containsExactly("samtrafikensales_latest.zip")
    }

    private fun createFetcher(
        timetableBlobPaths: Map<String, String> = emptyMap(),
        salesBlobPath: String? = this.salesBlobPath,
    ) = GcsFileFetcher(
        storage = storage,
        config = config,
        stopPlaceBlobPath = stopPlaceBlobPath,
        timetableBlobPaths = timetableBlobPaths,
        salesBlobPath = salesBlobPath,
        downloadDir = tempDir,
    )

    private fun createZipFile(
        path: Path,
        files: Map<String, String>,
    ) {
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
